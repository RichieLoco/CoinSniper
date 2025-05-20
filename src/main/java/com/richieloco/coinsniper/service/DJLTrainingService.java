package com.richieloco.coinsniper.service;

import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class  DJLTrainingService {

    public void train(List<TradeDecisionRecord> tradeData) {
        try (Model model = Model.newInstance("coin-sniper-model");
             NDManager manager = NDManager.newBaseManager()) {

            // Define the loss function
            //TODO picked this one as a default but need to research some more
            Loss loss = Loss.softmaxCrossEntropyLoss();

            // Create the optimizer
            Optimizer optimizer = Optimizer.adam().build();

            TrainingConfig config = new DefaultTrainingConfig(loss);

            try (Trainer trainer = model.newTrainer(config)) {
                log.info("Simulated training with {} records.", tradeData.size());
            }
        }
    }

    public void logToFile(List<TradeDecisionRecord> history) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("logs/training_log.tsv", true))) {
            writer.write("CoinSymbol\tExchange\tRiskScore\tExecuted\tTimestamp\n");
            for (TradeDecisionRecord td : history) {
                writer.write(String.format("%s\t%s\t%.2f\t%b\t%s\n",
                        td.getCoinSymbol(), td.getExchange(), td.getRiskScore(),
                        td.isTradeExecuted(), td.getTimestamp()));
            }
        } catch (IOException e) {
            log.error("Error writing training log", e);
        }
    }
}
