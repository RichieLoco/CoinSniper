package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

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
}
