package com.richieloco.coinsniper.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class TrainingResult {

    @Builder.Default
    private List<Double> lossPerEpoch = List.of();

    @Builder.Default
    private List<Double> accuracyPerEpoch = List.of();

    @Builder.Default
    private double averageAccuracy = 0.0;

    @Builder.Default
    private double finalLoss = 0.0;

    @Builder.Default
    private double finalAccuracy = 0.0;

    @Builder.Default
    private double averageLoss = 0.0;

    @Builder.Default
    private int epochs = 0;

    @Builder.Default
    private int batchSize = 0;

    @Builder.Default
    private String architecture = "Linear(1→8) → ReLU → Linear(8→1) → Sigmoid";

    @Builder.Default
    private String optimizer = "Adam (LR=0.001)";

    @Builder.Default
    private String modelSummary = "No model trained";

    @Builder.Default
    private String customSummary = "N/A";
}
