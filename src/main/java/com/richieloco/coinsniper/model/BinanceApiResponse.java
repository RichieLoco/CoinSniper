package com.richieloco.coinsniper.model;

import lombok.Data;
import java.util.List;

@Data
public class BinanceApiResponse {
    private BinanceData data;
    private boolean success;
    private String code;
}