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

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.client.ClientResponse.create;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({NoSecurityTestConfig.class})
@ActiveProfiles("test")
public class AnnouncementCallingIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testCallBinanceEndpoint() {
        var responseSpec = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/announcements/call")
                        .queryParam("type", 1)
                        .queryParam("pageNo", 1)
                        .queryParam("pageSize", 10)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();

        String responseBody = new String(responseSpec.getResponseBody(), StandardCharsets.UTF_8);
        assertNotNull(responseBody);

        System.out.println("Response body: " + responseBody);

        if (responseBody.trim().startsWith("[")) {
            // basic validation of list content
            if (responseBody.contains("title")) {
                // contains at least one announcement, check key fields exist
                webTestClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/api/announcements/call")
                                .queryParam("type", 1)
                                .queryParam("pageNo", 1)
                                .queryParam("pageSize", 10)
                                .build())
                        .exchange()
                        .expectStatus().isOk()
                        .expectBody()
                        .jsonPath("$[0].title").exists()
                        .jsonPath("$[0].coinSymbol").exists()
                        .jsonPath("$[0].delisting").isBoolean();
            } else {
                // array is empty or lacks announcements
                assertTrue(responseBody.equals("[]") || responseBody.length() < 10, "Expected empty or near-empty array");
            }
        } else {
            fail("Expected response to be a JSON array");
        }
    }

    @Test
    void testCallBinanceWithDefaults() {
        var responseSpec = webTestClient.get()
                .uri("/api/announcements/call")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .returnResult();

        String responseBody = new String(responseSpec.getResponseBody(), StandardCharsets.UTF_8);
        assertNotNull(responseBody);
    }

    @Test
    void testCallBinanceWithInvalidTypeParam() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/announcements/call")
                        .queryParam("type", "abc")
                        .queryParam("pageNo", 1)
                        .queryParam("pageSize", 10)
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @TestConfiguration
    static class MockWebClientConfig {

        @Bean
        public WebClient webClient() {
            ExchangeFunction exchangeFunction = request ->
                    Mono.just(create(OK)
                            .header("Content-Type", APPLICATION_JSON_VALUE)
                            .body("{\"data\":{\"catalogs\":[]}}")  // no articles
                            .build());

            return WebClient.builder()
                    .exchangeFunction(exchangeFunction)
                    .exchangeStrategies(ExchangeStrategies.withDefaults())
                    .build();
        }
    }
}
