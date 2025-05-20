package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.controller.AnnouncementController;
import com.richieloco.coinsniper.controller.TradeController;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.service.AnnouncementService;
import com.richieloco.coinsniper.service.TradeExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureWebTestClient
public class IntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Mock
    private AnnouncementService announcementService;

    @Mock
    private TradeExecutionService tradeExecutionService;

    @Test
    public void testPollBinanceEndpoint() {
        CoinAnnouncementRecord record = CoinAnnouncementRecord.builder()
                .id(UUID.randomUUID().toString())
                .coinSymbol("XYZ")
                .title("New Listing")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        when(announcementService.pollBinanceAnnouncements()).thenReturn(Flux.just(record));

        webTestClient.get().uri("/api/announcements/poll")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CoinAnnouncementRecord.class)
                .hasSize(1)
                .contains(record);
    }

    @Test
    public void testTradeExecutionEndpoint() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .id(UUID.randomUUID().toString())
                .coinSymbol("XYZ")
                .title("XYZ Listing")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        TradeDecisionRecord decision = TradeDecisionRecord.builder()
                .id(UUID.randomUUID())
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(3.2)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        when(tradeExecutionService.evaluateAndTrade(any(CoinAnnouncementRecord.class)))
                .thenReturn(Mono.just(decision));

        webTestClient.post()
                .uri("/api/trade/execute")
                .bodyValue(announcement)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.coinSymbol").isEqualTo("XYZ")
                .jsonPath("$.exchange").isEqualTo("Binance")
                .jsonPath("$.tradeExecuted").isEqualTo(true);
    }
}
