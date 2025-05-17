package com.richieloco.coinsniper.router;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.on.Risk;
import com.richieloco.coinsniper.handler.ExchangeHandler;
import com.richieloco.coinsniper.service.risk.ExchangeEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;

@WebFluxTest(controllers = RiskRouter.class, excludeAutoConfiguration = {
        ReactiveSecurityAutoConfiguration.class
})
@Import({RiskRouter.class, ExchangeHandler.class, RiskRouterTest.MockedBeans.class})
class RiskRouterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ExchangeEvaluationService service;

    @BeforeEach
    void resetMocks() {
        reset(service); // reset all stubbings and interactions

        Mockito.when(service.assessExchangeRisk(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Mono.just(new Risk(0.42)));

        Mockito.when(service.assessCoinRisk(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Mono.just(new Risk(0.88)));
    }

    @TestConfiguration
    static class MockedBeans {

        @Bean
        public ExchangeEvaluationService mockRiskEvaluationService() {
            ExchangeEvaluationService service = Mockito.mock(ExchangeEvaluationService.class);
            return service;
        }

        @Bean
        public CoinSniperConfig.BinanceConfig mockBinanceConfig() {
            CoinSniperConfig.BinanceConfig mock = Mockito.mock(CoinSniperConfig.BinanceConfig.class);
            Mockito.when(mock.getType()).thenReturn(1);
            Mockito.when(mock.getPageNo()).thenReturn(1);
            Mockito.when(mock.getPageSize()).thenReturn(10);
            return mock;
        }
    }

    @Test
    void testExchangeRiskEndpoint_Success() {
        webTestClient.get()
                .uri("/api/risk/exchange?from=Binance&to=Kraken&volatility=0.6&liquidity=0.2&fees=0.001")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Risk.class)
                .consumeWith(result -> {
                    Risk risk = result.getResponseBody();
                    assert risk != null;
                    assert risk.getRiskScore() == 0.42;
                });
    }

    @Test
    void testCoinRiskEndpoint_Success() {
        webTestClient.get()
                .uri("/api/risk/coin?coinA=BTC&coinB=ETH&volatility=0.5&correlation=0.8&volumeDiff=0.1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Risk.class)
                .consumeWith(result -> {
                    Risk score = result.getResponseBody();
                    assert score != null;
                    assert score.getRiskScore() == 0.88;
                });
    }

    @Test
    void testExchangeRiskEndpoint_MissingParams() {
        webTestClient.get()
                .uri("/api/risk/exchange?from=Binance&to=Kraken") // missing volatility, liquidity, fees
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testCoinRiskEndpoint_MissingParams() {
        webTestClient.get()
                .uri("/api/risk/coin?coinA=BTC&coinB=ETH") // missing volatility, correlation, volume
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testExchangeRiskEndpoint_InvalidParams() {
        webTestClient.get()
                .uri("/api/risk/exchange?from=Binance&to=Kraken&volatility=abc&liquidity=0.2&fees=0.001")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testCoinRiskEndpoint_InvalidParams() {
        webTestClient.get()
                .uri("/api/risk/coin?coinA=BTC&coinB=ETH&volatility=0.5&correlation=bad&volume=0.1")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void testExchangeRiskEndpoint_InternalServerError() {
        Mockito.when(service.assessExchangeRisk(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("AI failure"));

        webTestClient.get()
                .uri("/api/risk/exchange?from=Binance&to=Kraken&volatility=0.6&liquidity=0.2&fees=0.001").exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void testCoinRiskEndpoint_InternalServerError() {
        Mockito.when(service.assessCoinRisk(anyString(), anyString(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Model error"));

        webTestClient.get()
                .uri("/api/risk/coin?coinA=BTC&coinB=ETH&volatility=0.5&correlation=0.8&volumeDiff=0.1")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}
