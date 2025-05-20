package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.service.TradeExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class TradeControllerTest {

    @Mock
    private TradeExecutionService tradeExecutionService;

    private TradeController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new TradeController(tradeExecutionService);
    }

    @Test
    public void trade_shouldReturnDecision() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder()
                .coinSymbol("XYZ")
                .build();

        TradeDecisionRecord result = TradeDecisionRecord.builder()
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(4.2)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        when(tradeExecutionService.evaluateAndTrade(input)).thenReturn(Mono.just(result));

        StepVerifier.create(controller.trade(input))
                .expectNext(result)
                .verifyComplete();
    }

    @Test
    public void trade_shouldHandleEmptyResult() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("ABC").build();
        when(tradeExecutionService.evaluateAndTrade(input)).thenReturn(Mono.empty());

        StepVerifier.create(controller.trade(input))
                .verifyComplete();
    }
}
