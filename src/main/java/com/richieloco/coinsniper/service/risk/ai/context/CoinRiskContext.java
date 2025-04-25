package com.richieloco.coinsniper.service.risk.ai.context;

import java.util.Objects;

public record CoinRiskContext(
        String coinA,
        String coinB,
        double historicalVolatility, // average of both coins
        double correlation, // between -1 and 1
        double volumeDifference // normalized trading volume difference
) {
    public CoinRiskContext {
        Objects.requireNonNull(coinA);
        Objects.requireNonNull(coinB);
    }

    @Override
    public String toString() {
        return String.format(
                "Comparing %s vs %s with volatility %.2f, correlation %.2f, volume diff %.2f",
                coinA, coinB, historicalVolatility, correlation, volumeDifference
        );
    }
}
