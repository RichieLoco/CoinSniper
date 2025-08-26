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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class DJLTrainingService {

    private final TradeDecisionRepository tradeDecisionRepository;

    private static final String PYTHON_PATH = Paths.get(".venv", "Scripts", "python.exe").toString();
    private static final String MODEL_DIR = "models/coin-sniper";
    private static final String STAGING_DIR = "models/coin-sniper-staging";
    private static final String PYTHON_SCRIPT = Paths.get("src", "main", "resources", "scripts", "export_to_pt.py").toString();

    private static final int EPOCHS = 8;
    private static final int BATCH_SIZE = 8;

    public DJLTrainingService(TradeDecisionRepository tradeDecisionRepository) {
        this.tradeDecisionRepository = tradeDecisionRepository;
    }

    /* --------------------------- Public API --------------------------- */

    /** Train non‑blocking. Always returns a TrainingResult (errors are captured). */
    public Mono<TrainingResult> trainReactive(List<TradeDecisionRecord> tradeData) {
        return Mono.fromCallable(() -> trainBlocking(tradeData))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(lastTraining::set)
                .onErrorResume(ex -> {
                    log.warn("Training failed (graceful): {}", ex.getMessage());
                    TrainingResult fallback = TrainingResult.builder()
                            .lossPerEpoch(List.of())
                            .accuracyPerEpoch(List.of())
                            .averageAccuracy(0.0)
                            .modelSummary("Training failed: " + ex.getMessage())
                            .build();
                    lastTraining.set(fallback);
                    return Mono.just(fallback);
                });
    }

    /** Exposes last training result to the controller **/
    public TrainingResult getLastTrainingResult() {
        return lastTraining.get();
    }

    /** Predict risk for the latest record of coinSymbol (non‑blocking). */
    public Mono<PredictionResult> predict(String coinSymbol) {
        return tradeDecisionRepository.findTopByCoinSymbolOrderByTimestampDesc(coinSymbol)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No historical data for coin symbol: " + coinSymbol)))
                .flatMap(record ->
                        Mono.fromCallable(() -> runDjlPrediction(coinSymbol, record.getRiskScore()))
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }

    /* ------------------------ Training (blocking) --------------------- */

    private TrainingResult trainBlocking(List<TradeDecisionRecord> tradeData) {
        try (Model model = Model.newInstance("coin-sniper-model", "PyTorch");
             NDManager manager = NDManager.newBaseManager()) {

            // Network: 1 -> 8 -> 1 with sigmoid head (probability)
            SequentialBlock block = new SequentialBlock()
                    .add(Linear.builder().setUnits(8).build())
                    .add(Activation.reluBlock())
                    .add(Linear.builder().setUnits(1).build())
                    .add(Activation.sigmoidBlock());
            model.setBlock(block);

            // Build dataset: normalize risk 0..10 -> 0..1
            float[][] features;
            float[] labels;
            if (tradeData == null || tradeData.isEmpty()) {
                features = new float[][]{{0f}, {1f}};
                labels   = new float[]{0f, 1f};
            } else {
                features = new float[tradeData.size()][1];
                labels = new float[tradeData.size()];
                for (int i = 0; i < tradeData.size(); i++) {
                    double raw = tradeData.get(i).getRiskScore();
                    float x = (float) Math.max(0.0, Math.min(10.0, raw)) / 10.0f;
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
            Optimizer optimizer = Optimizer.adam()
                    .optLearningRateTracker(Tracker.fixed(0.001f))
                    .build();

            DefaultTrainingConfig config = new DefaultTrainingConfig(loss)
                    .optOptimizer(optimizer)
                    .addTrainingListeners(TrainingListener.Defaults.logging());

            List<Double> lossPerEpoch = new ArrayList<>();
            List<Double> accPerEpoch  = new ArrayList<>();

            try (Trainer trainer = model.newTrainer(config)) {
                trainer.initialize(manager.create(features).getShape());

                for (int epoch = 0; epoch < EPOCHS; epoch++) {
                    // Train epoch (no metrics computed here to avoid non‑existent APIs)
                    for (Batch batch : trainer.iterateDataset(dataset)) {
                        try (batch) {
                            EasyTrain.trainBatch(trainer, batch);
                            trainer.step();
                        }
                    }
                    trainer.notifyListeners(l -> l.onEpoch(trainer));

                    // Evaluate current model on in‑memory arrays via Predictor
                    EvalStats stats = evaluateOnMemory(model, features, labels);
                    lossPerEpoch.add(stats.loss());
                    accPerEpoch.add(stats.accuracy());
                }
            }

            /* Save to STAGING; optionally export/promote to PRODUCTION */
            try {
                Path stagingDir = Paths.get(STAGING_DIR);
                Files.createDirectories(stagingDir);
                model.save(stagingDir, "coin-sniper-model");
                log.info("Saved DJL model (staging) to {}", stagingDir.toAbsolutePath());

                if (isExportEnabled()) {
                    exportWeightsAsCsv(model, stagingDir);
                    runPythonExportScript(stagingDir);

                    Path prodDir = Paths.get(MODEL_DIR);
                    Files.createDirectories(prodDir);

                    promote(stagingDir.resolve("coin-sniper-model.params"), prodDir.resolve("coin-sniper-model.params"));
                    promote(stagingDir.resolve("coin-sniper-model.json"),   prodDir.resolve("coin-sniper-model.json"));
                    Path stagingPt = stagingDir.resolve("coin-sniper-model.pt");
                    if (Files.exists(stagingPt)) {
                        promote(stagingPt, prodDir.resolve("coin-sniper-model.pt"));
                    }
                } else {
                    log.info("Export disabled (coin-sniper.export.enabled=false) — skipping .pt/export/promote.");
                }
            } catch (Exception ioEx) {
                // Non‑fatal for tests/CI
                log.warn("Non-fatal IO during save/export: {}", ioEx.getMessage());
            }

            double avgAcc = accPerEpoch.stream().mapToDouble(d -> d).average().orElse(0.0);

            return TrainingResult.builder()
                    .lossPerEpoch(lossPerEpoch)
                    .accuracyPerEpoch(accPerEpoch)
                    .averageAccuracy(avgAcc)
                    .modelSummary(model.toString())
                    .build();

        } catch (IOException | TranslateException ex) {
            throw new RuntimeException("Training failed", ex);
        }
    }

    private final AtomicReference<TrainingResult> lastTraining = new AtomicReference<>(TrainingResult.builder()
            .modelSummary("Model has not yet been trained.")
            .lossPerEpoch(List.of())
            .accuracyPerEpoch(List.of())
            .averageAccuracy(0.0)
            .build());



    /* --------------------------- Prediction --------------------------- */

    private PredictionResult runDjlPrediction(String coinSymbol, Double riskScore)
            throws IOException, MalformedModelException, TranslateException {

        float x = (float) Math.max(0.0, Math.min(10.0, riskScore)) / 10.0f;

        // We do NOT require .pt — params/json are enough to load the DJL model.
        Path modelDir = Paths.get(MODEL_DIR);
        if (!Files.exists(modelDir)) {
            throw new IOException("Model directory not found: " + modelDir.toAbsolutePath());
        }

        try (Model model = Model.newInstance("coin-sniper-model", "PyTorch");
             NDManager ignore = NDManager.newBaseManager()) {

            model.load(modelDir, "coin-sniper-model");

            Translator<float[], Float> translator = new Translator<>() {
                @Override
                public NDList processInput(TranslatorContext ctx, float[] input) {
                    return new NDList(ctx.getNDManager().create(new float[][]{{input[0]}}));
                }
                @Override
                public Float processOutput(TranslatorContext ctx, NDList list) {
                    NDArray out = list.singletonOrThrow();     // could be [1,1] (batch x 1)
                    if (out.isScalar()) {
                        return out.getFloat();
                    }
                    float[] arr = out.toFloatArray();          // safe for any shape
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

    /* ---------------------- Evaluation Helpers ------------------------ */

    private record EvalStats(double loss, double accuracy) {}

    /** Evaluate loss/accuracy on in‑memory arrays via Predictor. */
    private EvalStats evaluateOnMemory(Model model, float[][] features, float[] labels) {
        try {
            Translator<float[], Float> translator = new Translator<>() {
                @Override
                public NDList processInput(TranslatorContext ctx, float[] input) {
                    return new NDList(ctx.getNDManager().create(new float[][]{{input[0]}}));
                }
                @Override
                public Float processOutput(TranslatorContext ctx, NDList list) {
                    NDArray out = list.singletonOrThrow();     // could be [1,1] (batch x 1)
                    if (out.isScalar()) {
                        return out.getFloat();
                    }
                    float[] arr = out.toFloatArray();          // safe for any shape
                    return arr.length > 0 ? arr[0] : Float.NaN;
                }

            };

            int correct = 0;
            double totalLoss = 0.0;
            final double eps = 1e-7;

            try (var predictor = model.newPredictor(translator)) {
                for (int i = 0; i < features.length; i++) {
                    float p = predictor.predict(new float[]{features[i][0]}); // [0,1]
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

    /* --------------------------- Utilities ---------------------------- */

    private static boolean isExportEnabled() {
        // Default false for tests/CI; enable in prod with -Dcoin-sniper.export.enabled=true
        return Boolean.getBoolean("coin-sniper.export.enabled");
    }

    private void promote(Path src, Path dst) throws IOException {
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignore) {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void exportWeightsAsCsv(Model model, Path baseDir) {
        try {
            Path weightsDir = baseDir.resolve("weights");
            Files.createDirectories(weightsDir);

            var children = model.getBlock().getChildren(); // [Linear, ReLU, Linear, Sigmoid]
            NDArray w1 = children.get(0).getValue().getParameters().get("weight").getArray();
            NDArray b1 = children.get(0).getValue().getParameters().get("bias").getArray();
            NDArray w2 = children.get(2).getValue().getParameters().get("weight").getArray();
            NDArray b2 = children.get(2).getValue().getParameters().get("bias").getArray();

            writeCsv(weightsDir.resolve("w1.csv"), w1.toFloatArray());
            writeCsv(weightsDir.resolve("b1.csv"), b1.toFloatArray());
            writeCsv(weightsDir.resolve("w2.csv"), w2.toFloatArray());
            writeCsv(weightsDir.resolve("b2.csv"), b2.toFloatArray());

            log.info("Exported weights to {}", weightsDir.toAbsolutePath());
        } catch (Exception ex) {
            log.error("Failed to export weights: {}", ex.getMessage(), ex);
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

    private void runPythonExportScript(Path modelDir) {
        try {
            log.info("Running Python export script with interpreter: {}", PYTHON_PATH);

            ProcessBuilder pb = new ProcessBuilder(PYTHON_PATH, PYTHON_SCRIPT, modelDir.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("[Python] {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python script failed with exit code {}", exitCode);
            }
        } catch (Exception ex) {
            log.error("Error running Python export script: {}", ex.getMessage(), ex);
        }
    }

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
