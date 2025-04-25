package com.richieloco.coinsniper.router;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureWebTestClient
class RiskRouterTest {

    @Autowired
    private WebTestClient client;

    @Test
    void testExchangeRiskEndpoint() {
        client.get()
                .uri("/api/risk/exchange?from=Binance&to=Kraken&volatility=0.6&liquidity=0.2&fees=0.001")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> assertTrue(response.matches("\\d\\.\\d+")));
    }
}
