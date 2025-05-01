package com.richieloco.coinsniper.entity.on.log;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class MarketTickerLog {
    String symbol;
    Double price;
    Instant timestamp;
}

