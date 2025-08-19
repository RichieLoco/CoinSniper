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
import ai.djl.training.*;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.training.dataset.Batch;
import ai.djl.training.dataset.Dataset;
import ai.djl.training.listener.TrainingListener;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import com.richieloco.coinsniper.dto.PredictionResult;
import com.richieloco.coinsniper.dto.TrainingResult;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class DJLTrainingService {

    private final TradeDecisionRepository tradeDecisionRepository;

    private static final String PYTHON_PATH = Paths.get(".venv", "Scripts", "python.exe").toString();
    private static final String MODEL_DIR = "models/coin-sniper";
    private static final String PYTHON_SCRIPT = Paths.get("src", "main", "resources", "scripts", "export_to_pt.py").toString();
    private static final int EPOCHS = 5;
    private static final int BATCH_SIZE = 8;

    public DJLTrainingService(TradeDecisionRepository tradeDecisionRepository) {
        this.tradeDecisionRepository = tradeDecisionRepository;
    }

    public TrainingResult train(List<TradeDecisionRecord> tradeData) {
        try (Model model = Model.newInstance("coin-sniper-model", "PyTorch");
             NDManager manager = NDManager.newBaseManager()) {

            SequentialBlock block = new SequentialBlock()
                    .add(Linear.builder().setUnits(8).build())
                    .add(Activation.reluBlock())
                    .add(Linear.builder().setUnits(2).build()); // Binary classification
            model.setBlock(block);

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

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(manager.create(features).getShape());

                for (int epoch = 0; epoch < EPOCHS; epoch++) {
                    try {
                        for (Batch batch : trainer.iterateDataset(dataset)) {
                            try (batch) {
                                EasyTrain.trainBatch(trainer, batch);
                                trainer.step();
                            }
                        }
                        trainer.notifyListeners(l -> l.onEpoch(trainer));
                    } catch (Exception e) {
                        log.warn("Training error in epoch {}: {}", epoch, e.getMessage());
                    }
                }
            }

            // Save model and export
            Files.createDirectories(Paths.get(MODEL_DIR));
            model.save(Paths.get(MODEL_DIR), "coin-sniper-model");
            log.info("Saved DJL model to {}", Paths.get(MODEL_DIR).toAbsolutePath());

            exportWeightsAsCsv(model);
            runPythonExportScript();

            return TrainingResult.builder()
                    .lossPerEpoch(List.of())
                    .accuracyPerEpoch(List.of())
                    .averageAccuracy(0.0)
                    .modelSummary(model.toString())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Training failed", e);
        }
    }

    public Mono<PredictionResult> predict(String coinSymbol) {
        return tradeDecisionRepository.findTopByCoinSymbolOrderByTimestampDesc(coinSymbol)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No historical data for coin symbol: " + coinSymbol)))
                .flatMap(record ->
                        Mono.fromCallable(() -> runDjlPrediction(coinSymbol, record.getRiskScore()))
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    private PredictionResult runDjlPrediction(String coinSymbol, Double riskScore) throws IOException, MalformedModelException {
        Path modelDir = Paths.get(MODEL_DIR);
        Path ptPath = modelDir.resolve("coin-sniper-model.pt");

        if (!Files.exists(ptPath)) {
            throw new IOException("Model file not found: " + ptPath.toAbsolutePath());
        }

        try (Model model = Model.newInstance("coin-sniper-model", "PyTorch")) {
            model.load(modelDir, "coin-sniper-model");

            try (NDManager manager = NDManager.newBaseManager()) {
                NDArray input = manager.create(new float[][]{{riskScore.floatValue()}});
                ParameterStore ps = new ParameterStore(manager, false);
                NDList output = model.getBlock().forward(ps, new NDList(input), false);

                NDArray probabilities = output.singletonOrThrow().softmax(1);
                float predictedRisk = probabilities.get(0, 1).getFloat(); // Class 1: "execute"

                return PredictionResult.builder()
                        .coinSymbol(coinSymbol)
                        .riskScore(predictedRisk * 10)
                        .notes("Predicted at " + Instant.now() + " using last known risk: " + riskScore)
                        .build();
            }
        }
    }

    private void exportWeightsAsCsv(Model model) {
        try {
            Path weightsDir = Paths.get(MODEL_DIR, "weights");
            Files.createDirectories(weightsDir);

            var children = model.getBlock().getChildren();
            NDArray w1 = children.get(0).getValue().getParameters().get("weight").getArray();
            NDArray b1 = children.get(0).getValue().getParameters().get("bias").getArray();
            NDArray w2 = children.get(2).getValue().getParameters().get("weight").getArray();
            NDArray b2 = children.get(2).getValue().getParameters().get("bias").getArray();

            writeCsv(weightsDir.resolve("w1.csv"), w1.toFloatArray());
            writeCsv(weightsDir.resolve("b1.csv"), b1.toFloatArray());
            writeCsv(weightsDir.resolve("w2.csv"), w2.toFloatArray());
            writeCsv(weightsDir.resolve("b2.csv"), b2.toFloatArray());

            log.info("Exported weights to {}", weightsDir.toAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to export weights: {}", e.getMessage(), e);
        }
    }

    private void writeCsv(Path filePath, float[] array) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (int i = 0; i < array.length; i++) {
                writer.write(Float.toString(array[i]));
                if (i < array.length - 1) writer.write(",");
            }
        }
    }

    private void runPythonExportScript() {
        try {
            log.info("Running Python export script with interpreter: {}", PYTHON_PATH);

            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, PYTHON_SCRIPT, MODEL_DIR);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("Python script finished successfully.");
            } else {
                log.error("Python script failed with exit code {}", exitCode);
            }

        } catch (Exception e) {
            log.error("Error running Python export script: {}", e.getMessage(), e);
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