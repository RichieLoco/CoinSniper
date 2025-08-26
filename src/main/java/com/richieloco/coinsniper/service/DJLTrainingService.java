package com.richieloco.coinsniper.service;

import ai.djl.Model;
import ai.djl.MalformedModelException;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.nn.Activation;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.dataset.Batch;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import com.richieloco.coinsniper.dto.PredictionResult;
import com.richieloco.coinsniper.dto.TrainingResult;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class DJLTrainingService {

    private final List<Double> cumulativeLossPerEpoch = new ArrayList<>();
    private final List<Double> cumulativeAccuracyPerEpoch = new ArrayList<>();

    private static final String MODEL_DIR = "models/coin-sniper";
    private static final int EPOCHS = 8;
    private static final int BATCH_SIZE = 8;

    private final TradeDecisionRepository tradeDecisionRepository;

    public DJLTrainingService(TradeDecisionRepository tradeDecisionRepository) {
        this.tradeDecisionRepository = tradeDecisionRepository;
    }

    @Getter
    private TrainingResult lastTrainingResult = TrainingResult.builder()
            .lossPerEpoch(new ArrayList<>())
            .accuracyPerEpoch(new ArrayList<>())
            .averageAccuracy(0.0)
            .modelSummary("")
            .build();

    public Mono<TrainingResult> trainReactive(List<TradeDecisionRecord> history) {
        return Mono.fromCallable(() -> {
            // Actually perform the training
            TrainingResult result = trainBlocking(history);

            // Append new metrics to cumulative lists
            cumulativeLossPerEpoch.addAll(result.getLossPerEpoch());
            cumulativeAccuracyPerEpoch.addAll(result.getAccuracyPerEpoch());

            double avgAccuracy = cumulativeAccuracyPerEpoch.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);

            lastTrainingResult = TrainingResult.builder()
                    .lossPerEpoch(new ArrayList<>(cumulativeLossPerEpoch))
                    .accuracyPerEpoch(new ArrayList<>(cumulativeAccuracyPerEpoch))
                    .averageAccuracy(avgAccuracy)
                    .modelSummary("Trained for total " + cumulativeLossPerEpoch.size() + " epochs.")
                    .build();

            return lastTrainingResult;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<PredictionResult> predict(String coinSymbol) {
        return tradeDecisionRepository.findTopByCoinSymbolOrderByTimestampDesc(coinSymbol)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No historical data for coin symbol: " + coinSymbol)))
                .flatMap(record -> Mono.fromCallable(() -> runDjlPrediction(coinSymbol, record.getRiskScore()))
                        .subscribeOn(Schedulers.boundedElastic()));
    }

    private TrainingResult trainBlocking(List<TradeDecisionRecord> tradeData) {
        try (Model model = Model.newInstance("coin-sniper-model", "PyTorch");
             NDManager manager = NDManager.newBaseManager()) {

            SequentialBlock block = new SequentialBlock()
                    .add(Linear.builder().setUnits(8).build())
                    .add(Activation.reluBlock())
                    .add(Linear.builder().setUnits(1).build())
                    .add(Activation.sigmoidBlock());
            model.setBlock(block);

            float[][] features;
            float[] labels;
            if (tradeData == null || tradeData.isEmpty()) {
                features = new float[][]{{0f}, {1f}};
                labels = new float[]{0f, 1f};
            } else {
                features = new float[tradeData.size()][1];
                labels = new float[tradeData.size()];
                for (int i = 0; i < tradeData.size(); i++) {
                    float x = (float) Math.max(0.0, Math.min(10.0, tradeData.get(i).getRiskScore())) / 10.0f;
                    features[i][0] = x;
                    labels[i] = tradeData.get(i).isTradeExecuted() ? 1f : 0f;
                }
            }

            Dataset dataset = new ArrayDataset.Builder()
                    .setData(manager.create(features))
                    .optLabels(manager.create(labels).toType(DataType.FLOAT32, false))
                    .setSampling(BATCH_SIZE, true)
                    .build();

            Loss loss = Loss.sigmoidBinaryCrossEntropyLoss();
            Optimizer optimizer = Optimizer.adam().optLearningRateTracker(Tracker.fixed(0.001f)).build();
            DefaultTrainingConfig config = new DefaultTrainingConfig(loss)
                    .optOptimizer(optimizer)
                    .addTrainingListeners(TrainingListener.Defaults.logging());

            List<Double> lossPerEpoch = new ArrayList<>();
            List<Double> accPerEpoch = new ArrayList<>();

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(manager.create(features).getShape());
                for (int epoch = 0; epoch < EPOCHS; epoch++) {
                    for (Batch batch : trainer.iterateDataset(dataset)) {
                        try (batch) {
                            EasyTrain.trainBatch(trainer, batch);
                            trainer.step();
                        }
                    }
                    trainer.notifyListeners(listener -> listener.onEpoch(trainer));
                    EvalStats stats = evaluateOnMemory(model, features, labels);
                    lossPerEpoch.add(stats.loss());
                    accPerEpoch.add(stats.accuracy());
                }
            }

            double avgAcc = accPerEpoch.stream().mapToDouble(d -> d).average().orElse(0.0);

            return TrainingResult.builder()
                    .lossPerEpoch(lossPerEpoch)
                    .accuracyPerEpoch(accPerEpoch)
                    .averageAccuracy(avgAcc)
                    .modelSummary(model.toString())
                    .build();

        } catch (IOException | TranslateException e) {
            throw new RuntimeException("Training failed", e);
        }
    }

    private PredictionResult runDjlPrediction(String coinSymbol, Double riskScore)
            throws IOException, MalformedModelException, TranslateException {
        float x = (float) Math.max(0.0, Math.min(10.0, riskScore)) / 10.0f;
        Path modelDir = Path.of(MODEL_DIR);
        if (!Files.exists(modelDir)) throw new IOException("Model directory not found: " + modelDir);

        try (Model model = Model.newInstance("coin-sniper-model", "PyTorch")) {
            model.load(modelDir, "coin-sniper-model");

            Translator<float[], Float> translator = new Translator<>() {
                public NDList processInput(TranslatorContext ctx, float[] input) {
                    return new NDList(ctx.getNDManager().create(new float[][]{{input[0]}}));
                }
                public Float processOutput(TranslatorContext ctx, NDList list) {
                    NDArray out = list.singletonOrThrow();
                    if (out.isScalar()) return out.getFloat();
                    float[] arr = out.toFloatArray();
                    return arr.length > 0 ? arr[0] : Float.NaN;
                }
            };

            try (var predictor = model.newPredictor(translator)) {
                float prob = predictor.predict(new float[]{x});
                return PredictionResult.builder()
                        .coinSymbol(coinSymbol)
                        .riskScore(prob * 10f)
                        .notes("Predicted at " + Instant.now() + " using normalized risk=" + x)
                        .build();
            }
        }
    }

    private EvalStats evaluateOnMemory(Model model, float[][] features, float[] labels) {
        try {
            Translator<float[], Float> translator = new Translator<>() {
                public NDList processInput(TranslatorContext ctx, float[] input) {
                    return new NDList(ctx.getNDManager().create(new float[][]{{input[0]}}));
                }
                public Float processOutput(TranslatorContext ctx, NDList list) {
                    NDArray out = list.singletonOrThrow();
                    if (out.isScalar()) return out.getFloat();
                    float[] arr = out.toFloatArray();
                    return arr.length > 0 ? arr[0] : Float.NaN;
                }
            };

            int correct = 0;
            double totalLoss = 0.0;
            final double eps = 1e-7;

            try (var predictor = model.newPredictor(translator)) {
                for (int i = 0; i < features.length; i++) {
                    float p = predictor.predict(new float[]{features[i][0]});
                    float y = labels[i];

                    double lp = Math.min(Math.max(p, (float) eps), 1.0 - eps);
                    double l = -(y * Math.log(lp) + (1 - y) * Math.log(1 - lp));
                    totalLoss += l;

                    int predLbl = p >= 0.5f ? 1 : 0;
                    if (predLbl == (int) y) correct++;
                }
            }

            double avgLoss = features.length > 0 ? (totalLoss / features.length) : 0.0;
            double acc = features.length > 0 ? (double) correct / features.length : 0.0;
            return new EvalStats(avgLoss, acc);

        } catch (TranslateException e) {
            log.warn("Evaluation failed: {}", e.getMessage());
            return new EvalStats(0.0, 0.0);
        }
    }

    private List<Double> concat(List<Double> a, List<Double> b) {
        List<Double> result = new ArrayList<>(a != null ? a : List.of());
        if (b != null) result.addAll(b);
        return result;
    }

    private record EvalStats(double loss, double accuracy) {}

    /* --------------------------- Logging ------------------------------ */

    public void logToFile(List<TradeDecisionRecord> history) {
        logToFile(history, "logs/training_log.tsv");
    }

    public void logToFile(List<TradeDecisionRecord> history, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("CoinSymbol\tExchange\tRiskScore\tExecuted\tTimestamp\n");
            for (TradeDecisionRecord td : history) {
                writer.write(String.format("%s\t%s\t%.2f\t%b\t%s\n",
                        td.getCoinSymbol(), td.getExchange(), td.getRiskScore(),
                        td.isTradeExecuted(), td.getTimestamp()));
            }
        } catch (IOException ex) {
            log.error("Error writing training log", ex);
        }
    }

}
