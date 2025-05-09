package com.richieloco.coinsniper.service.risk;

import com.richieloco.coinsniper.entity.on.Risk;
import com.richieloco.coinsniper.service.risk.ai.CoinRiskAssessor;
import com.richieloco.coinsniper.service.risk.ai.ExchangeRiskAssessor;
import com.richieloco.coinsniper.service.risk.ai.context.CoinRiskContext;
import com.richieloco.coinsniper.service.risk.ai.context.ExchangeRiskContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class RiskEvaluationServiceTest {

    private ExchangeRiskAssessor exchangeRiskAssessor;
    private CoinRiskAssessor coinRiskAssessor;
    private RiskEvaluationService service;

    @BeforeEach
    void setUp() {
        exchangeRiskAssessor = mock(ExchangeRiskAssessor.class);
        coinRiskAssessor = mock(CoinRiskAssessor.class);
        service = new RiskEvaluationService(exchangeRiskAssessor, coinRiskAssessor);
    }

    // ✅ Successful exchange risk evaluation
    @Test
    void testAssessExchangeRisk_Success() {
        Risk spy = spy(Risk.class);
        spy.setRiskScore(0.75);
        when(exchangeRiskAssessor.assessRisk(any(ExchangeRiskContext.class))).thenReturn(Mono.just(spy));

        Risk result = service.assessExchangeRisk("Binance", "Kraken", 0.6, 0.2, 0.001).block();

        assertEquals(0.75, result.getRiskScore());
        verify(exchangeRiskAssessor, times(1)).assessRisk(any(ExchangeRiskContext.class));
    }

    // ✅ Successful coin risk evaluation
    @Test
    void testAssessCoinRisk_Success() {
        Risk spy = spy(Risk.class);
        spy.setRiskScore(0.55);
        when(coinRiskAssessor.assessRisk(any(CoinRiskContext.class))).thenReturn(Mono.just(spy));

        Risk result = service.assessCoinRisk("BTC", "ETH", 0.5, 0.3, 0.1).block();

        assertEquals(0.55, result.getRiskScore());
        verify(coinRiskAssessor, times(1)).assessRisk(any(CoinRiskContext.class));
    }

    // ❌ Simulated failure in exchange risk evaluation
    @Test
    void testAssessExchangeRisk_Exception() {
        when(exchangeRiskAssessor.assessRisk(any(ExchangeRiskContext.class)))
                .thenThrow(new RuntimeException("Error during exchange risk assessment"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.assessExchangeRisk("Binance", "Kraken", 0.6, 0.2, 0.001));

        assertEquals("Error during exchange risk assessment", ex.getMessage());
    }

    // ❌ Simulated failure in coin risk evaluation
    @Test
    void testAssessCoinRisk_Exception() {
        when(coinRiskAssessor.assessRisk(any(CoinRiskContext.class)))
                .thenThrow(new RuntimeException("Coin risk failure"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.assessCoinRisk("BTC", "ETH", 0.5, 0.3, 0.1));

        assertEquals("Coin risk failure", ex.getMessage());
    }
}
