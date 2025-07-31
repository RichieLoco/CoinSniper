package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.CoinSniperMockTestConfig;
import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({NoSecurityTestConfig.class, CoinSniperMockTestConfig.class})
@ComponentScan(
        basePackages = "com.richieloco.coinsniper",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ExchangeAssessor.class
        )
)
public class TradeExecutionIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TradeDecisionRepository repository;

    @BeforeEach
    public void clearRepo() {
        repository.deleteAll().block();
    }

    @Test
    public void postTrade_shouldReturnTradeDecision() {
        CoinAnnouncementRecord input = CoinAnnouncementRecord.builder()
                .coinSymbol("XYZ")
                .build();

        webTestClient.post()
                .uri("/api/trade/execute")
                .bodyValue(input)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].coinSymbol").isEqualTo("XYZ")
                .jsonPath("$[0].tradeExecuted").isBoolean()
                .jsonPath("$[0].exchange").isEqualTo("Binance")
                .jsonPath("$[0].riskScore").isEqualTo(4.0);
    }
}
