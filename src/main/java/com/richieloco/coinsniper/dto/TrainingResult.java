package com.richieloco.coinsniper.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TrainingResult {
    private List<Double> lossPerEpoch;
    private List<Double> accuracyPerEpoch;
    private double averageAccuracy;
    private String modelSummary;
}
