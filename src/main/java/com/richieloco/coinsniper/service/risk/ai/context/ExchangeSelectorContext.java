package com.richieloco.coinsniper.service.risk.ai.context;

import java.util.Objects;

public record ExchangeSelectorContext(
        String exchanges,
        String targetCoin,
        String stableCoins
) {
    public ExchangeSelectorContext {
        Objects.requireNonNull(exchanges);
        Objects.requireNonNull(targetCoin);
        Objects.requireNonNull(stableCoins);
    }

    @Override
    public String toString() {
        return String.format(
                "Selecting from list of exchanges: %s that list coin: %s trading against stable coin(s): %s",
                exchanges, targetCoin, stableCoins
        );
    }
}
