package com.richieloco.coinsniper.service.on;

import com.richieloco.coinsniper.entity.on.log.TradeOrderLog;
import com.richieloco.coinsniper.util.Exchanges;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public interface TradeService<T> {

    Mono<UUID> executeOrder(Exchanges exchange, TradeOrderLog.OrderSide orderSide, String symbol, double positionSize);
}