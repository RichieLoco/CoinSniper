package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DJLTrainingServiceTest {

    @BeforeAll
    public static void setUp() {
        System.setProperty("ai.djl.default_engine", "PyTorch");
    }

    @Test
    public void testTrainingAndLogging() {
        DJLTrainingService trainer = new DJLTrainingService();

        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .id(UUID.randomUUID())
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
}
