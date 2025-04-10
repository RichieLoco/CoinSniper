package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.on.TradeRequest;
import com.richieloco.coinsniper.entity.on.TradeResponse;
import com.richieloco.coinsniper.service.on.impl.TradeServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/binance")
@RequiredArgsConstructor
@Validated
public class OnExchangeTradeController {

    private final TradeServiceImpl tradeService;
    private final CoinSniperConfig.ExchangeConfig apiConfig;

    @PostMapping("/trade")
    Mono<TradeResponse> executeTrade(TradeRequest.OrderSide order) {
        return null;
    }
}
