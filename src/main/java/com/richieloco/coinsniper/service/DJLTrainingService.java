package com.richieloco.coinsniper.service;

import ai.djl.*;
import ai.djl.ndarray.*;
import ai.djl.ndarray.types.*;
import ai.djl.nn.*;
import ai.djl.nn.core.*;
import ai.djl.training.*;
import ai.djl.training.dataset.*;
import ai.djl.training.listener.*;
import ai.djl.training.loss.*;
import ai.djl.training.optimizer.*;
import ai.djl.training.tracker.*;
import ai.djl.translate.*;
import com.richieloco.coinsniper.dto.PredictionResult;
import com.richieloco.coinsniper.dto.TrainingResult;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class DJLTrainingService {

    private static final int EPOCHS = 8;
    private static final int BATCH_SIZE = 8;

    private final TradeDecisionRepository tradeDecisionRepository;

    private final List<Double> cumulativeLossPerEpoch = new ArrayList<>();
    private final List<Double> cumulativeAccuracyPerEpoch = new ArrayList<>();

    @Getter
    private TrainingResult lastTrainingResult = TrainingResult.builder().build();

    public DJLTrainingService(TradeDecisionRepository tradeDecisionRepository) {
        this.tradeDecisionRepository = tradeDecisionRepository;
    }

    public Mono<TrainingResult> trainReactive(List<TradeDecisionRecord> history) {
        return Mono.fromCallable(() -> {
            TrainingResult result = trainBlocking(history);

            cumulativeLossPerEpoch.addAll(result.getLossPerEpoch());
            cumulativeAccuracyPerEpoch.addAll(result.getAccuracyPerEpoch());

            double avgAccuracy = cumulativeAccuracyPerEpoch.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double avgLoss = cumulativeLossPerEpoch.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            StringBuilder summary = new StringBuilder();
            summary.append("Trained for total ")
                    .append(cumulativeLossPerEpoch.size())
                    .append(" epochs.\n\n")
                    .append("Model Block Summary:\n")
                    .append(result.getModelSummary());

            lastTrainingResult = TrainingResult.builder()
                    .lossPerEpoch(new ArrayList<>(cumulativeLossPerEpoch))
                    .accuracyPerEpoch(new ArrayList<>(cumulativeAccuracyPerEpoch))
                    .averageAccuracy(avgAccuracy)
                    .averageLoss(avgLoss)
                    .finalLoss(result.getFinalLoss())
                    .finalAccuracy(result.getFinalAccuracy())
                    .epochs(cumulativeLossPerEpoch.size())
                    .batchSize(BATCH_SIZE)
                    .architecture("Linear(1→8) → ReLU → Linear(8→1) → Sigmoid")
                    .optimizer("Adam (LR=0.001)")
                    .customSummary(String.format("""
                                DJL Training Summary (Cumulative):
                                Architecture: %s
                                Optimizer: %s
                                Total Epochs: %d
                                Batch Size: %d
                                Final Loss: %.4f
                                Final Accuracy: %.2f%%
                                Avg Accuracy: %.2f%%
                            """,
                            "Linear(1→8) → ReLU → Linear(8→1) → Sigmoid",
                            "Adam (LR=0.001)",
                            cumulativeLossPerEpoch.size(),
                            BATCH_SIZE,
                            result.getFinalLoss(),
                            result.getFinalAccuracy() * 100,
                            avgAccuracy * 100
                    ))
                    .build();

            return lastTrainingResult;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<PredictionResult> predict(String coinSymbol) {
        return tradeDecisionRepository.findTopByCoinSymbolOrderByDecidedAtDesc(coinSymbol)
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

            float[][] features = tradeData.isEmpty() ? new float[][]{{0f}, {1f}} : new float[tradeData.size()][1];
            float[] labels = tradeData.isEmpty() ? new float[]{0f, 1f} : new float[tradeData.size()];

            for (int i = 0; i < tradeData.size(); i++) {
                features[i][0] = (float) Math.max(0.0, Math.min(10.0, tradeData.get(i).getRiskScore())) / 10.0f;
                labels[i] = tradeData.get(i).isTradeExecuted() ? 1f : 0f;
            }

            Dataset dataset = new ArrayDataset.Builder()
                    .setData(manager.create(features))
                    .optLabels(manager.create(labels).toType(DataType.FLOAT32, false))
                    .setSampling(BATCH_SIZE, true)
                    .build();

            Loss loss = Loss.sigmoidBinaryCrossEntropyLoss();
            Optimizer optimizer = Optimizer.adam().optLearningRateTracker(Tracker.fixed(0.001f)).build();
            DefaultTrainingConfig config = new DefaultTrainingConfig(loss).optOptimizer(optimizer).addTrainingListeners(TrainingListener.Defaults.logging());

            List<Double> lossPerEpoch = new ArrayList<>();
            List<Double> accPerEpoch = new ArrayList<>();

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(new Shape(1, 1));
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

            double avgAcc = accPerEpoch.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

            return TrainingResult.builder()
                    .lossPerEpoch(lossPerEpoch)
                    .accuracyPerEpoch(accPerEpoch)
                    .averageAccuracy(avgAcc)
                    .finalLoss(lossPerEpoch.get(lossPerEpoch.size() - 1))
                    .finalAccuracy(accPerEpoch.get(accPerEpoch.size() - 1))
                    .averageLoss(lossPerEpoch.stream().mapToDouble(Double::doubleValue).average().orElse(0.0))
                    .epochs(EPOCHS)
                    .batchSize(BATCH_SIZE)
                    .architecture("Linear(1→8) → ReLU → Linear(8→1) → Sigmoid")
                    .optimizer("Adam (LR=0.001)")
                    .modelSummary(model.getBlock().toString())
                    .build();

        } catch (IOException | TranslateException e) {
            throw new RuntimeException("Training failed", e);
        }
    }

    private PredictionResult runDjlPrediction(String coinSymbol, Double riskScore)
            throws IOException, MalformedModelException, TranslateException {
        float x = (float) Math.max(0.0, Math.min(10.0, riskScore)) / 10.0f;
        try (Model model = Model.newInstance("coin-sniper-model", "PyTorch")) {
            model.setBlock(new SequentialBlock()
                    .add(Linear.builder().setUnits(8).build())
                    .add(Activation.reluBlock())
                    .add(Linear.builder().setUnits(1).build())
                    .add(Activation.sigmoidBlock()));

            try (Trainer trainer = model.newTrainer(new DefaultTrainingConfig(Loss.sigmoidBinaryCrossEntropyLoss()))) {
                trainer.initialize(new Shape(1, 1));
            }

            Translator<float[], Float> translator = new Translator<>() {
                public NDList processInput(TranslatorContext ctx, float[] input) {
                    return new NDList(ctx.getNDManager().create(new float[][]{{input[0]}}));
                }
                public Float processOutput(TranslatorContext ctx, NDList list) {
                    NDArray out = list.singletonOrThrow();
                    return out.isScalar() ? out.getFloat() : out.toFloatArray()[0];
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
                    return out.isScalar() ? out.getFloat() : out.toFloatArray()[0];
                }
            };

            int correct = 0;
            double totalLoss = 0.0;
            final double eps = 1e-7;

            try (var predictor = model.newPredictor(translator)) {
                for (int i = 0; i < features.length; i++) {
                    float p = predictor.predict(new float[]{features[i][0]});
                    float y = labels[i];
                    double lp = Math.min(Math.max(p, eps), 1.0 - eps);
                    double l = -(y * Math.log(lp) + (1 - y) * Math.log(1 - lp));
                    totalLoss += l;
                    if ((p >= 0.5f ? 1 : 0) == (int) y) correct++;
                }
            }

            return new EvalStats(totalLoss / features.length, (double) correct / features.length);

        } catch (TranslateException e) {
            log.warn("Evaluation failed: {}", e.getMessage());
            return new EvalStats(0.0, 0.0);
        }
    }

    private record EvalStats(double loss, double accuracy) {}

    public void logToFile(List<TradeDecisionRecord> history, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("CoinSymbol\tExchange\tRiskScore\tExecuted\tTimestamp\n");
            for (TradeDecisionRecord td : history) {
                writer.write("%s\t%s\t%.2f\t%b\t%s\n".formatted(
                        td.getCoinSymbol(), td.getExchange(), td.getRiskScore(),
                        td.isTradeExecuted(), td.getDecidedAt()));
            }
        } catch (IOException ex) {
            log.error("Error writing training log", ex);
        }
    }
}
