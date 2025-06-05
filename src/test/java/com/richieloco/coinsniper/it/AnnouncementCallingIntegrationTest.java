package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.NoSecurityTestConfig;
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

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.client.ClientResponse.create;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({NoSecurityTestConfig.class, AnnouncementCallingIntegrationTest.MockWebClientConfig.class})
@ActiveProfiles("test")
public class AnnouncementCallingIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCallBinanceEndpoint() {
        webTestClient.get()
                .uri("/api/announcements/call")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(response -> {
                    String body = new String(response.getResponseBody());
                    System.out.println("Mocked Response: " + body);
                    // Add assertions if needed
                });
    }

    @TestConfiguration
    static class MockWebClientConfig {

        @Bean
        public WebClient webClient() {
            ExchangeFunction exchangeFunction = request ->
                    Mono.just(create(OK)
                            .header("Content-Type", APPLICATION_JSON_VALUE)
                            .body("{\"mock\":\"binance response\"}")
                            .build());

            return WebClient.builder()
                    .exchangeFunction(exchangeFunction)
                    .exchangeStrategies(ExchangeStrategies.withDefaults())
                    .build();
        }
    }
}