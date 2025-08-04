package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.dto.TrainingResult;
import com.richieloco.coinsniper.dto.PredictionResult;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.DJLTrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class BacktestingController {

    private final TradeDecisionRepository tradeDecisionRepository;
    private final DJLTrainingService djlTrainingService;

    @GetMapping("/backtesting")
    public Mono<String> backtesting(Model model) {
        return tradeDecisionRepository.findAll()
                .collectList()
                .flatMap(history -> {
                    model.addAttribute("history", history);

                    TrainingResult metrics;
                    try {
                        metrics = djlTrainingService.train(history);
                        djlTrainingService.logToFile(history);
                    } catch (Exception e) {
                        log.error("Training failed", e);
                        metrics = TrainingResult.builder()
                                .lossPerEpoch(List.of())
                                .accuracyPerEpoch(List.of())
                                .averageAccuracy(0.0)
                                .modelSummary("Training failed: " + e.getMessage())
                                .build();
                        model.addAttribute("error", "Training failed: " + e.getMessage());
                    }

                    model.addAttribute("metrics", metrics);
                    return Mono.just("backtesting");
                });
    }


    @PostMapping("/backtesting/predict")
    public String predict(@ModelAttribute("coinSymbol") String coinSymbol, Model model) {
        log.debug("Coin Symbol received: {}", coinSymbol);
        PredictionResult prediction = djlTrainingService.predict(coinSymbol);
        model.addAttribute("prediction", prediction);
        return "backtesting";
    }

}
