package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.entity.*;
import com.richieloco.coinsniper.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;

@SpringBootTest
@AutoConfigureWebTestClient
@Import(NoSecurityTestConfig.class)
public class DashboardIT {

    @Autowired private WebTestClient webTestClient;

    @Autowired private TradeDecisionRepository tradeRepo;
    @Autowired private CoinAnnouncementRepository announcementRepo;
    @Autowired private ExchangeAssessmentRepository assessmentRepo;
    @Autowired private ErrorResponseRepository errorRepo;

    @BeforeEach
    void setup() {
        Instant now = Instant.now();

        tradeRepo.deleteAll().then(
                tradeRepo.save(
                        TradeDecisionRecord.builder()
                                .coinSymbol("XYZ")
                                .exchange("Binance")
                                .riskScore(3.5)
                                .tradeExecuted(true)
                                .timestamp(now)
                                .build()
                )
        ).block();

        announcementRepo.deleteAll().then(
                announcementRepo.save(
                        CoinAnnouncementRecord.builder()
                                .coinSymbol("XYZ")
                                .title("New Coin XYZ Listed")
                                .announcedAt(now)
                                .build()
                )
        ).block();

        assessmentRepo.deleteAll().then(
                assessmentRepo.save(
                        ExchangeAssessmentRecord.builder()
                                .coinListing("XYZUSDT")
                                .exchange("Binance")
                                .overallRiskScore("MEDIUM")
                                .assessedAt(now)
                                .build()
                )
        ).block();

        errorRepo.deleteAll().then(
                errorRepo.save(
                        ErrorResponseRecord.builder()
                                .source("Binance")
                                .statusCode(500)
                                .errorMessage("Internal Server Error")
                                .timestamp(now)
                                .build()
                )
        ).block();
    }

    @Test
    public void testDashboardViewLoads_withAllData() {
        webTestClient.get().uri("/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assert body != null;
                    assert body.contains("XYZ");
                    assert body.contains("New Coin XYZ Listed");
                    assert body.contains("Internal Server Error");
                });
    }

    @Test
    public void testDashboardViewLoadsWithNoData() {
        tradeRepo.deleteAll().then(
                announcementRepo.deleteAll().then(
                        assessmentRepo.deleteAll().then(
                                errorRepo.deleteAll()))).block();

        webTestClient.get().uri("/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(response -> {
                    String body = response.getResponseBody();
                    assert body != null;
                    assert !body.contains("XYZ");
                });
    }
}
