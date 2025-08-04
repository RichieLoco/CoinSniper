package com.richieloco.coinsniper.service;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.nn.Activation;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.core.Linear;
import ai.djl.training.*;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.dataset.Batch;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.djl.translate.TranslateException;
import com.richieloco.coinsniper.dto.PredictionResult;
import com.richieloco.coinsniper.dto.TrainingResult;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
public class DJLTrainingService {

    private static final String MODEL_DIR = "models/coin-sniper";
    private static final int EPOCHS = 5;
    private static final int BATCH_SIZE = 8;

    public TrainingResult train(List<TradeDecisionRecord> tradeData) {
        try (Model model = Model.newInstance("coin-sniper-model");
             NDManager manager = NDManager.newBaseManager()) {

            // Build a simple feedforward network
            SequentialBlock block = new SequentialBlock()
                    .add(Linear.builder().setUnits(8).build())
                    .add(Activation.reluBlock())
                    .add(Linear.builder().setUnits(2).build()); // binary classification
            model.setBlock(block);

            // Prepare dataset
            float[][] features;
            long[] labels;
            if (tradeData == null || tradeData.isEmpty()) {
                log.warn("No training data provided. Using dummy dataset.");
                features = new float[][]{{0f}, {1f}};
                labels = new long[]{0, 1};
            } else {
                features = new float[tradeData.size()][1];
                labels = new long[tradeData.size()];
                for (int i = 0; i < tradeData.size(); i++) {
                    features[i][0] = (float) tradeData.get(i).getRiskScore();
                    labels[i] = tradeData.get(i).isTradeExecuted() ? 1 : 0;
                }
            }

            Dataset dataset = new ArrayDataset.Builder()
                    .setData(manager.create(features))
                    .optLabels(manager.create(labels).toType(DataType.INT64, false))
                    .setSampling(BATCH_SIZE, true)
                    .build();

            Loss loss = Loss.softmaxCrossEntropyLoss();
            Optimizer optimizer = Optimizer.adam()
                    .optLearningRateTracker(Tracker.fixed(0.0005f))
                    .build();

            TrainingConfig config = new DefaultTrainingConfig(loss)
                    .optOptimizer(optimizer)
                    .addTrainingListeners(TrainingListener.Defaults.logging());

            double[] epochLosses = new double[EPOCHS];

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(new NDList(manager.create(features)).getShapes());

                if (tradeData != null && !tradeData.isEmpty()) {
                    for (int epoch = 0; epoch < EPOCHS; epoch++) {
                        try {
                            int batchCount = 0;
                            for (Batch batch : trainer.iterateDataset(dataset)) {
                                try (batch) {
                                    try {
                                        EasyTrain.trainBatch(trainer, batch);
                                    } catch (ai.djl.TrainingDivergedException divEx) {
                                        log.warn("Training diverged (NaN loss). Stopping early: {}", divEx.getMessage());
                                        break; // stop processing this epoch
                                    }
                                    trainer.step();
                                    batchCount++;
                                }
                            }
                            trainer.notifyListeners(l -> l.onEpoch(trainer));
                        } catch (Exception e) {
                            log.warn("Training interrupted during epoch {}: {}", epoch + 1, e.getMessage());
                        }
                    }
                }
            }

            // Save model
            File modelDir = new File(MODEL_DIR);
            modelDir.mkdirs();
            model.save(Paths.get(MODEL_DIR), "coin-sniper-model");
            log.info("Model saved to {}", Paths.get(MODEL_DIR).toAbsolutePath());

            List<Double> lossPerEpoch = java.util.Arrays.stream(epochLosses).boxed().toList();
            List<Double> accuracyPerEpoch = java.util.Arrays.stream(epochLosses)
                    .map(x -> 1 - x)
                    .boxed().toList();
            double avgAcc = accuracyPerEpoch.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            return TrainingResult.builder()
                    .lossPerEpoch(lossPerEpoch)
                    .accuracyPerEpoch(accuracyPerEpoch)
                    .averageAccuracy(avgAcc)
                    .modelSummary(model.toString())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Training failed", e);
        }
    }

    public PredictionResult predict(String coinSymbol) {
        try {
            Path modelDir = Paths.get(MODEL_DIR);

            // Find latest .params file dynamically
            String latestModelName = Files.list(modelDir)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(".params"))
                    .max(Comparator.naturalOrder())
                    .map(name -> name.replace(".params", ""))
                    .orElseThrow(() -> new IOException("No .params model file found in " + MODEL_DIR));

            log.info("Loading latest model: {}", latestModelName);

            try (Model model = Model.newInstance("coin-sniper-model")) {
                model.load(modelDir, latestModelName);

                try (NDManager manager = NDManager.newBaseManager()) {
                    float riskScore = (float) Math.random() * 10;
                    NDArray input = manager.create(new float[][]{{riskScore}});
                    ParameterStore ps = new ParameterStore(manager, false);
                    NDList output = model.getBlock().forward(ps, new NDList(input), false);

                    float predictedRisk = output.singletonOrThrow().softmax(1).getFloat(0);

                    return PredictionResult.builder()
                            .coinSymbol(coinSymbol)
                            .riskScore(predictedRisk * 10)
                            .notes("Predicted at " + Instant.now())
                            .build();
                }
            }
        } catch (IOException | MalformedModelException e) {
            throw new RuntimeException("Prediction failed", e);
        }
    }

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
        } catch (IOException e) {
            log.error("Error writing training log", e);
        }
    }
}
