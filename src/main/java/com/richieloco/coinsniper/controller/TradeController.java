package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.service.TradeExecutionService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/trade")
@AllArgsConstructor
public class TradeController {

    private final TradeExecutionService tradeExecutionService;

    @PostMapping("/execute")
    public Mono<TradeDecisionRecord> trade(@RequestBody CoinAnnouncementRecord announcement) {
        if (announcement == null) {
            return Mono.error(new IllegalArgumentException("Announcement cannot be null"));
        }
        return tradeExecutionService.evaluateAndTrade(announcement);
    }
}
