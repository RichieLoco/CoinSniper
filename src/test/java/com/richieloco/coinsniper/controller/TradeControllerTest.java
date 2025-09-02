package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.service.TradeExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Objects;

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
                .decidedAt(Instant.now())
                .build();

        when(tradeExecutionService.evaluateAndTrade(input)).thenReturn(Flux.just(result));

        StepVerifier.create(controller.trade(input))
                .expectNext(result)
                .verifyComplete();
    }

    @Test
    public void trade_shouldHandleEmptyResult() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("ABC").build();
        when(tradeExecutionService.evaluateAndTrade(input)).thenReturn(Flux.empty());

        StepVerifier.create(controller.trade(input))
                .verifyComplete();
    }

    @Test
    public void trade_shouldCallServiceWithCorrectInput() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("DEF").build();
        TradeDecisionRecord result = TradeDecisionRecord.builder().coinSymbol("DEF").build();

        when(tradeExecutionService.evaluateAndTrade(any())).thenReturn(Flux.just(result));

        StepVerifier.create(controller.trade(input))
                .expectNext(result)
                .verifyComplete();

        verify(tradeExecutionService, times(1)).evaluateAndTrade(eq(input));
    }

    @Test
    public void trade_shouldHandleNullMono() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("NULL").build();

        // Simulate misbehaving service returning Flux.empty()
        when(tradeExecutionService.evaluateAndTrade(input))
                .thenReturn(Flux.empty());


        StepVerifier.create(controller.trade(input))
                .verifyComplete();
    }

    @Test
    public void trade_shouldPropagateErrors() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder().coinSymbol("ERR").build();

        when(tradeExecutionService.evaluateAndTrade(input))
                .thenReturn(Flux.error(new RuntimeException("Service failure")));

        StepVerifier.create(controller.trade(input))
                .expectErrorMessage("Service failure")
                .verify();
    }

    @Test
    public void trade_shouldRejectNullInput() {
        StepVerifier.create(controller.trade(null))
                .expectErrorMessage("Announcement cannot be null")
                .verify();
    }

    @Test
    public void trade_shouldNotCallServiceOnNullInput() {
        try {
            controller.trade(null).subscribe();
        } catch (Exception ignored) {}

        verify(tradeExecutionService, never()).evaluateAndTrade(any());
    }

    @Test
    public void trade_shouldHandleInvalidAnnouncementStructure() {
        CoinAnnouncementRecord incomplete = CoinAnnouncementRecord.builder().build();

        when(tradeExecutionService.evaluateAndTrade(incomplete)).thenReturn(Flux.empty());

        StepVerifier.create(controller.trade(incomplete))
                .verifyComplete();
    }

    @Test
    public void trade_shouldReturnDifferentDecisionsPerCoin() {
        CoinAnnouncementRecord input1 = CoinAnnouncementRecord.builder().coinSymbol("AAA").build();
        CoinAnnouncementRecord input2 = CoinAnnouncementRecord.builder().coinSymbol("BBB").build();

        TradeDecisionRecord result1 = TradeDecisionRecord.builder().coinSymbol("AAA").build();
        TradeDecisionRecord result2 = TradeDecisionRecord.builder().coinSymbol("BBB").build();

        when(tradeExecutionService.evaluateAndTrade(input1)).thenReturn(Flux.just(result1));
        when(tradeExecutionService.evaluateAndTrade(input2)).thenReturn(Flux.just(result2));

        StepVerifier.create(controller.trade(input1))
                .expectNext(result1)
                .verifyComplete();

        StepVerifier.create(controller.trade(input2))
                .expectNext(result2)
                .verifyComplete();
    }
}
