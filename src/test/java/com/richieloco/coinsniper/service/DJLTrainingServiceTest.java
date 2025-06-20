package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DJLTrainingServiceTest {

    @BeforeAll
    public static void setupEngine() {
        System.setProperty("ai.djl.default_engine", "PyTorch");
    }

    @Test
    public void testTrainingAndLogging() {
        DJLTrainingService trainer = new DJLTrainingService();


        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .coinSymbol("XYZ")
                .exchange("Binance")
                .riskScore(3.5)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        List<TradeDecisionRecord> data = List.of(record);

        trainer.train(data);
        trainer.logToFile(data);
    }

    @Test
    public void testTrainingAndLogging_withEmptyList() {
        DJLTrainingService trainer = new DJLTrainingService();

        List<TradeDecisionRecord> data = List.of();

        trainer.train(data); // Should not throw
        trainer.logToFile(data); // Should create header line only
    }

    @Test
    public void testLogToFile_createsExpectedContent() throws IOException {
        DJLTrainingService trainer = new DJLTrainingService();

        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .coinSymbol("TEST")
                .exchange("MockEx")
                .riskScore(2.5)
                .tradeExecuted(false)
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        List<TradeDecisionRecord> data = List.of(record);

        Path tempLog = Files.createTempFile("unittest_training_log", ".tsv");
        System.setProperty("log.override.path", tempLog.toAbsolutePath().toString()); // Optional if you redirect

        trainer.logToFile(data, tempLog.toAbsolutePath().toString());
        String output = Files.readString(tempLog);

        assertTrue(output.contains("CoinSymbol\tExchange\tRiskScore\tExecuted\tTimestamp"));
        assertTrue(output.contains("TEST\tMockEx\t2.50\tfalse\t2024-01-01T00:00:00Z"));

        Files.deleteIfExists(tempLog);
    }

    @Test
    public void testTrain_handlesExceptionGracefully() {
        DJLTrainingService trainer = new DJLTrainingService();

        // Invalid block simulation (e.g., no layers)
        TradeDecisionRecord badRecord = TradeDecisionRecord.builder()
                .coinSymbol(null) // malformed data, although not used in this logic
                .exchange(null)
                .riskScore(Double.NaN)
                .tradeExecuted(true)
                .timestamp(null)
                .build();

        trainer.train(List.of(badRecord)); // Should not throw due to internal try-catch
    }
}
