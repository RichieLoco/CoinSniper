package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.UUID;

@SpringBootTest
@AutoConfigureWebTestClient
@Import(NoSecurityTestConfig.class)
@ActiveProfiles("test")
public class DashboardIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TradeDecisionRepository repository;

    @BeforeEach
    void setup() {
        repository.deleteAll().then(
                repository.save(
                        TradeDecisionRecord.builder()
                                .coinSymbol("XYZ")
                                .exchange("Binance")
                                .riskScore(3.5)
                                .tradeExecuted(true)
                                .timestamp(Instant.now())
                                .build()
                )
        ).block();
    }

    @Test
    public void testDashboardViewLoads() {
        webTestClient.get()
                .uri("/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assert body != null && body.contains("XYZ");
                });
    }
}

