package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureWebTestClient
public class DashboardIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TradeDecisionRepository repository;

    @Test
    public void testDashboardViewLoads() {
        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .id(UUID.randomUUID())
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(3.5)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        when(repository.findAll()).thenReturn(Flux.just(record));

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
