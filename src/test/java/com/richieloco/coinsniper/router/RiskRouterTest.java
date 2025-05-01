package com.richieloco.coinsniper.router;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.on.RiskScore;
import com.richieloco.coinsniper.handler.RiskHandler;
import com.richieloco.coinsniper.service.risk.RiskEvaluationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = RiskRouter.class, excludeAutoConfiguration = {
        ReactiveSecurityAutoConfiguration.class
})
@Import({RiskRouter.class, RiskHandler.class, RiskRouterTest.MockedBeans.class})

class RiskRouterTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private RiskEvaluationService service;

    @Autowired
    private CoinSniperConfig.BinanceConfig config;

    @TestConfiguration
    static class MockedBeans {

        @Bean
        public RiskEvaluationService mockRiskEvaluationService() {
            return Mockito.mock(RiskEvaluationService.class);
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
    void testExchangeRiskEndpoint() {
        webTestClient.get()
                .uri("/api/risk/exchange?from=Binance&to=Kraken&volatility=0.6&liquidity=0.2&fees=0.001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(RiskScore.class);
                //.value(response -> assertTrue(response.matches("\\d\\.\\d+")));

    }
}
