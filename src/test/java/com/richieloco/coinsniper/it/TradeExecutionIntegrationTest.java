package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.CoinSniperMockTestConfig;
import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Import({NoSecurityTestConfig.class, CoinSniperMockTestConfig.class})
@ActiveProfiles("test")
@ComponentScan(
        basePackages = "com.richieloco.coinsniper",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ExchangeAssessor.class
        )
)
public class TradeExecutionIntegrationTest {

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
                .jsonPath("$.coinSymbol").isEqualTo("XYZ")
                .jsonPath("$.exchange").isEqualTo("Binance")
                .jsonPath("$.riskScore").isEqualTo(4.0)
                .jsonPath("$.tradeExecuted").isEqualTo(true);
    }
}
