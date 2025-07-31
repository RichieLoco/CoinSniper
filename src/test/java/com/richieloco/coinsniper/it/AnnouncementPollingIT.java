package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.service.AnnouncementPollingScheduler;
import com.richieloco.coinsniper.service.TradeExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.client.ClientResponse.create;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({NoSecurityTestConfig.class, AnnouncementPollingIT.MockWebClientConfig.class})
public class AnnouncementPollingIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private AnnouncementPollingScheduler scheduler;

    private static final String AUTH_USER = "admin";
    private static final String AUTH_PASS = "changeme";

    @Test
    void sanityCheckPollingDirectly() throws InterruptedException {
        scheduler.startPolling();
        Thread.sleep(1000); // Let it poll once
        assertTrue(scheduler.isPollingActive(), "Polling should be active");
        scheduler.stopPolling();
    }

    @Test
    void testStartStopStatus() {
        webTestClient.post().uri("/api/announcements/poll/start")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk();

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                webTestClient.get().uri("/api/announcements/poll/status")
                        .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .value(body -> assertTrue(body != null && body.contains("active"), "Expected status to be 'active'"))
        );

        webTestClient.post().uri("/api/announcements/poll/stop")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk();

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                webTestClient.get().uri("/api/announcements/poll/status")
                        .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .value(body -> assertTrue(body != null && body.contains("stopped"), "Expected status to be 'stopped'"))
        );
    }

    @Test
    void testDoubleStartDoesNotError() {
        webTestClient.post().uri("/api/announcements/poll/start")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri("/api/announcements/poll/start")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk();

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                webTestClient.get().uri("/api/announcements/poll/status")
                        .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .value(body -> assertTrue(body != null && body.contains("active"), "Expected status to be 'active'"))
        );
    }

    @Test
    void testDoubleStopDoesNotError() {
        webTestClient.post().uri("/api/announcements/poll/start")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri("/api/announcements/poll/stop")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk();

        webTestClient.post().uri("/api/announcements/poll/stop")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk();

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() ->
                webTestClient.get().uri("/api/announcements/poll/status")
                        .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody(String.class)
                        .value(body -> assertTrue(body != null && body.contains("stopped"), "Expected status to be 'stopped'"))
        );
    }

    @Test
    void testStatusBeforeStartShouldBeStopped() {
        webTestClient.get().uri("/api/announcements/poll/status")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertTrue(body != null && body.contains("stopped"), "Expected status to be 'stopped'"));
    }

    @Test
    void testCallBinanceEndpointTriggersTrade() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .coinSymbol("MCK")
                .title("Binance Will List MockCoin (MCK)")
                .announcedAt(Instant.ofEpochMilli(1717866000000L))
                .delisting(false)
                .build();

        webTestClient.post()
                .uri("/api/trade/execute")
                .headers(h -> h.setBasicAuth(AUTH_USER, AUTH_PASS))
                .bodyValue(announcement)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].coinSymbol").isEqualTo("MCK")
                .jsonPath("$[0].tradeExecuted").isBoolean();
    }

    @TestConfiguration
    static class MockWebClientConfig {
        @Bean
        public WebClient announcementPollingWebClient() {
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
                public Flux<TradeDecisionRecord> evaluateAndTrade(CoinAnnouncementRecord announcement) {
                    return Flux.just(
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
