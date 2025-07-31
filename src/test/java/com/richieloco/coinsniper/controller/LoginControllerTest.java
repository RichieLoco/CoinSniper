package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(LoginController.class)
@Import(NoSecurityTestConfig.class)
class LoginControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("✅ /login should return OK and render login view")
    void loginPage_ShouldReturnLoginView() {
        webTestClient.get()
                .uri("/login")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    // Assert body contains unique part of the login page
                    assert body != null && body.contains("login-form");
                });
    }

    @Test
    @DisplayName("❌ Invalid path should return 404")
    void invalidPath_ShouldReturnNotFound() {
        webTestClient.get()
                .uri("/invalid-path")
                .exchange()
                .expectStatus().isNotFound();
    }
}
