package com.richieloco.coinsniper.service.on.impl;

import com.richieloco.coinsniper.entity.on.TradeRequest;
import com.richieloco.coinsniper.entity.on.TradeResponse;
import com.richieloco.coinsniper.service.on.TradeService;
import com.richieloco.coinsniper.util.Exchanges;
import reactor.core.publisher.Mono;

public class TradeServiceImpl implements TradeService<TradeResponse> {

    @Override
    public Mono<TradeResponse> executeOrder(Exchanges exchange, TradeRequest.OrderSide orderSide, String symbol, double positionSize) {
        return null;
    }
}
