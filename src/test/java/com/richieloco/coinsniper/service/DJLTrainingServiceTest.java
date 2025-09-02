package com.richieloco.coinsniper.service;

import ai.djl.TrainingDivergedException;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DJLTrainingServiceTest {

    private static DJLTrainingService trainer;

    @BeforeAll
    public static void setUp() {
        System.setProperty("ai.djl.default_engine", "PyTorch");
        System.setProperty("coin-sniper.export.enabled", "false");

        TradeDecisionRepository mockRepo = Mockito.mock(TradeDecisionRepository.class);
        trainer = new DJLTrainingService(mockRepo);
    }

    @Test
    public void testTrainingAndLogging() {
        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(3.5)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        List<TradeDecisionRecord> data = List.of(record);

        // Reactive train — will not throw thanks to onErrorResume
        trainer.trainReactive(data).block();
        trainer.logToFile(data, "");
    }

    @Test
    public void testTrainingAndLogging_withEmptyList() {
        List<TradeDecisionRecord> data = List.of();

        // Should not throw even if DJL cannot run on CI
        trainer.trainReactive(data).block();
        trainer.logToFile(data, "");
    }

    @Test
    public void testTrain_handlesExceptionGracefully() {
        TradeDecisionRecord badRecord = TradeDecisionRecord.builder()
                .riskScore(Double.NaN)
                .tradeExecuted(true)
                .build();

        assertThrows(TrainingDivergedException.class, () -> {
            trainer.trainReactive(List.of(badRecord)).block();
        });
    }

    @Test
    public void testLogToFile_createsExpectedContent() throws IOException {
        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .coinSymbol("TEST")
                .exchange("MockEx")
                .riskScore(2.5)
                .tradeExecuted(false)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        List<TradeDecisionRecord> data = List.of(record);

        Path tempLog = Files.createTempFile("unittest_training_log", ".tsv");
        System.setProperty("log.override.path", tempLog.toAbsolutePath().toString()); // if you handle this in your code

        trainer.logToFile(data, tempLog.toAbsolutePath().toString());
        String output = Files.readString(tempLog);

        assertTrue(output.contains("CoinSymbol\tExchange\tRiskScore\tExecuted\tTimestamp"));
        assertTrue(output.contains("TEST\tMockEx\t2.50\tfalse\t2024-01-01T00:00:00Z"));

        Files.deleteIfExists(tempLog);
    }
}
