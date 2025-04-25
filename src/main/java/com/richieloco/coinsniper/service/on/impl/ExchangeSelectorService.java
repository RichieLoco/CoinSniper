package com.richieloco.coinsniper.service.on.impl;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.util.Exchanges;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExchangeSelectorService {

    private final CoinSniperConfig config;
    private final Exchanges exchanges;

    // selectExchange method
    // - return subset of Exchanges? list of em? one at a time?
    // - use AI to access risk of trading on exchange based on volumes, liquidity?


}