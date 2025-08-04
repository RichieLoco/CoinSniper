package com.richieloco.coinsniper.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PredictionResult {
    private String coinSymbol;
    private double riskScore;
    private String notes;
}
