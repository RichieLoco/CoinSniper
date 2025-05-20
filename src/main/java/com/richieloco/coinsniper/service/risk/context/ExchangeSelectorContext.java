package com.richieloco.coinsniper.service.risk.context;

import com.richieloco.coinsniper.config.CoinSniperConfig;

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

    public static ExchangeSelectorContext from(CoinSniperConfig config, String coinSymbol) {
        String exchanges = String.join(",", config.getSupported().getExchanges());
        String stableCoins = String.join(",", config.getSupported().getStableCoins());
        return new ExchangeSelectorContext(exchanges, coinSymbol, stableCoins);
    }

    @Override
    public String toString() {
        return String.format(
                "Selecting from list of exchanges: %s that list coin: %s trading against stable coin(s): %s",
                exchanges, targetCoin, stableCoins
        );
    }
}