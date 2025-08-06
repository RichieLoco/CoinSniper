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
    private String modelSummary = "No model trained";
}

