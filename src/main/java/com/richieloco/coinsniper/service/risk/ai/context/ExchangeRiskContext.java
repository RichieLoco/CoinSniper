package com.richieloco.coinsniper.service.risk.ai.context;

import java.util.Objects;

public record ExchangeRiskContext(
        String fromExchange,
        String toExchange,
        double marketVolatility, // e.g., 0.35 = 35% volatility
        double liquidityDifference, // e.g., 0.20 = 20% lower liquidity in target
        double feeDifference // e.g., 0.001 = 0.1% higher fees in target
) {
    public ExchangeRiskContext {
        Objects.requireNonNull(fromExchange);
        Objects.requireNonNull(toExchange);
    }

    @Override
    public String toString() {
        return String.format(
                "Trading from %s to %s with market volatility %.2f, liquidity diff %.2f, fee diff %.4f",
                fromExchange, toExchange, marketVolatility, liquidityDifference, feeDifference
        );
    }
}
