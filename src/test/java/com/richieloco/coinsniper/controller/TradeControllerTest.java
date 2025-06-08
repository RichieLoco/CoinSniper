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

    @Test
    public void trade_shouldCallServiceWithCorrectInput() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("DEF").build();
        TradeDecisionRecord result = TradeDecisionRecord.builder().coinSymbol("DEF").build();

        when(tradeExecutionService.evaluateAndTrade(any())).thenReturn(Mono.just(result));

        StepVerifier.create(controller.trade(input))
                .expectNext(result)
                .verifyComplete();

        verify(tradeExecutionService, times(1)).evaluateAndTrade(eq(input));
    }

    @Test
    public void trade_shouldHandleNullMono() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("NULL").build();

        // Simulate misbehaving service returning Mono.just(null)
        when(tradeExecutionService.evaluateAndTrade(input)).thenReturn(Mono.justOrEmpty(null));

        StepVerifier.create(controller.trade(input))
                .verifyComplete();
    }

    @Test
    public void trade_shouldPropagateErrors() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("ERR").build();

        when(tradeExecutionService.evaluateAndTrade(input))
                .thenReturn(Mono.error(new RuntimeException("Service failure")));

        StepVerifier.create(controller.trade(input))
                .expectErrorMessage("Service failure")
                .verify();
    }
}
