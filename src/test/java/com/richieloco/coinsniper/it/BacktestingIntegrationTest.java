package com.richieloco.coinsniper.it;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.DJLTrainingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureWebTestClient
public class BacktestingIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ChatModel chatModel;

    @Mock
    private TradeDecisionRepository repository;

    @Mock
    private DJLTrainingService trainingService;

    @Test
    public void testBacktestingViewLoadsAndLogs() {
        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .id(UUID.randomUUID())
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(3.2)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        // Mocking repository response
        when(repository.findAll()).thenReturn(Flux.just(record));

        webTestClient.get()
                .uri("/backtesting")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .consumeWith(result -> {
                    String body = result.getResponseBody();
                    assert body != null;
                    assert body.contains("XYZ");
                });

        verify(trainingService).train(anyList());
        verify(trainingService).logToFile(anyList());
    }
}
