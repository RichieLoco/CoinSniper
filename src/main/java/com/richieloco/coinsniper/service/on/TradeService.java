package com.richieloco.coinsniper.service.on;

import com.richieloco.coinsniper.entity.on.TradeRequest;
import com.richieloco.coinsniper.entity.on.TradeResponse;
import com.richieloco.coinsniper.util.Exchanges;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public interface TradeService<T> {

    Mono<TradeResponse> executeOrder(Exchanges exchange, TradeRequest.OrderSide orderSide, String symbol, double positionSize);
}