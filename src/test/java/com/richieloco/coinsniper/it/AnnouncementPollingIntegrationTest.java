package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.service.TradeExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.client.ClientResponse.create;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({NoSecurityTestConfig.class, AnnouncementPollingIntegrationTest.MockWebClientConfig.class})
@ActiveProfiles("test")
public class AnnouncementPollingIntegrationTest {

    @Autowired
    private WebTestClient client;

    @Test
    void testStartStopStatus() {
        client.post().uri("/api/announcements/poll/start")
                .exchange()
                .expectStatus().isOk();

        client.get().uri("/api/announcements/poll/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String response = result.getResponseBody();
                    assert response != null && response.contains("active");
                });

        client.post().uri("/api/announcements/poll/stop")
                .exchange()
                .expectStatus().isOk();

        client.get().uri("/api/announcements/poll/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String response = result.getResponseBody();
                    assert response != null && response.contains("stopped");
                });
    }

    @Test
    void testDoubleStartDoesNotError() {
        client.post().uri("/api/announcements/poll/start").exchange().expectStatus().isOk();
        client.post().uri("/api/announcements/poll/start").exchange().expectStatus().isOk();

        client.get().uri("/api/announcements/poll/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null && body.contains("active");
                });
    }

    @Test
    void testDoubleStopDoesNotError() {
        client.post().uri("/api/announcements/poll/start").exchange().expectStatus().isOk();
        client.post().uri("/api/announcements/poll/stop").exchange().expectStatus().isOk();
        client.post().uri("/api/announcements/poll/stop").exchange().expectStatus().isOk();

        client.get().uri("/api/announcements/poll/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null && body.contains("stopped");
                });
    }

    @Test
    void testStatusBeforeStartShouldBeStopped() {
        client.get().uri("/api/announcements/poll/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null && body.contains("stopped");
                });
    }

    @Test
    void testCallBinanceEndpointTriggersTrade() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .coinSymbol("MCK")
                .title("Binance Will List MockCoin (MCK)")
                .announcedAt(Instant.ofEpochMilli(1717866000000L))
                .delisting(false)
                .build();

        client.post()
                .uri("/api/trade/execute")
                .bodyValue(announcement)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.coinSymbol").isEqualTo("MCK")
                .jsonPath("$.tradeExecuted").isBoolean();
    }

    @TestConfiguration
    static class MockWebClientConfig {

        @Bean
        public WebClient webClient() {
            ExchangeFunction exchangeFunction = request ->
                    Mono.just(create(OK)
                            .header("Content-Type", APPLICATION_JSON_VALUE)
                            .body("{\"data\":{\"catalogs\":[{\"catalogName\":\"New Cryptocurrency Listing\",\"articles\":[{\"title\":\"Binance Will List MockCoin (MCK)\",\"releaseDate\":1717866000000}]}]}}")
                            .build());

            return WebClient.builder()
                    .exchangeFunction(exchangeFunction)
                    .exchangeStrategies(ExchangeStrategies.withDefaults())
                    .build();
        }

        @Bean
        public TradeExecutionService tradeExecutionService() {
            return new TradeExecutionService(null, null, null) {
                @Override
                public Mono<TradeDecisionRecord> evaluateAndTrade(CoinAnnouncementRecord announcement) {
                    return Mono.just(
                            TradeDecisionRecord.builder()
                                    .coinSymbol(announcement.getCoinSymbol())
                                    .exchange("Binance")
                                    .riskScore(4.0)
                                    .tradeExecuted(true)
                                    .timestamp(Instant.now())
                                    .build()
                    );
                }
            };
        }

    }
}