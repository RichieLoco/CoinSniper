package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.DJLTrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureWebTestClient
@Import({
        NoSecurityTestConfig.class,
        BacktestingIT.TestMockConfig.class
})
public class BacktestingIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private TradeDecisionRepository repository;

    @Autowired
    private DJLTrainingService trainingService;

    @BeforeEach
    public void setUp() {
        repository.deleteAll().block();

        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(3.2)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        repository.save(record).block();
    }

    @Test
    public void testBacktestingViewLoadsAndLogs() {
        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(3.2)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        webTestClient.get()
                .uri("/backtesting")
                //.headers(headers -> headers.setBasicAuth("test", "test"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("XYZ");
                });

        verify(trainingService, atLeastOnce()).trainReactive(anyList());
        verify(trainingService, atLeastOnce()).logToFile(anyList());

    }

    @Test
    public void testBacktestingViewWithNoData() {
        repository.deleteAll().block();

        webTestClient.get()
                .uri("/backtesting")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert !body.contains("XYZ");
                });

        verify(trainingService, atLeastOnce()).trainReactive(anyList());
        verify(trainingService, atLeastOnce()).logToFile(anyList());
    }

    @Test
    public void testBacktestingViewWithMultipleTrades() {
        TradeDecisionRecord extra = TradeDecisionRecord.builder()
                .coinSymbol("ETH")
                .exchange("Bybit")
                .riskScore(2.1)
                .tradeExecuted(false)
                .timestamp(Instant.now())
                .build();

        repository.save(extra).block();

        webTestClient.get()
                .uri("/backtesting")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("XYZ");
                    assert body.contains("ETH");
                });

        verify(trainingService, atLeastOnce()).trainReactive(anyList());
        verify(trainingService, atLeastOnce()).logToFile(anyList());

    }

    @Test
    public void testBacktestingViewHandlesTrainingError() {
        // Simulate exception on train()
        Mockito.doThrow(new RuntimeException("Training failed"))
                .when(trainingService).trainReactive(anyList());

        webTestClient.get()
                .uri("/backtesting")
                .exchange()
                .expectStatus().isOk(); // app should still respond OK
    }

    @TestConfiguration
    static class TestMockConfig {
        @Primary
        @Bean
        public DJLTrainingService trainingService() {
            return Mockito.mock(DJLTrainingService.class);
        }
    }
}
