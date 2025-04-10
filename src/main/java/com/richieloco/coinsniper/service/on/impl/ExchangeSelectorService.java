package com.richieloco.coinsniper.service.on.impl;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.util.Exchanges;

public class ExchangeSelectorService {

    private final CoinSniperConfig.ExchangeConfig config;
    private final Exchanges exchanges;

    public ExchangeSelectorService(CoinSniperConfig.ExchangeConfig config, Exchanges exchanges) {
        this.config = config;
        this.exchanges = exchanges;
    }

    // selectExchange method
    // - return subset of Exchanges? list of em? one at a time?
}