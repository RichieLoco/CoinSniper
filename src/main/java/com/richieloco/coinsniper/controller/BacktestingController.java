package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.dto.TrainingResult;
import com.richieloco.coinsniper.dto.PredictionForm;
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
                                .modelSummary("Training failed: " + e.getMessage())
                                .build();
                        model.addAttribute("error", "Training failed: " + e.getMessage());
                    }

                    model.addAttribute("metrics", metrics != null ? metrics : TrainingResult.builder().build());
                    model.addAttribute("predictionForm", new PredictionForm()); //... Add empty form
                    return Mono.just("backtesting");
                });
    }

    @PostMapping("/backtesting/predict")
    public Mono<String> predict(@ModelAttribute PredictionForm predictionForm, Model model) {
        String coinSymbol = predictionForm.getCoinSymbol();
        log.debug("Coin Symbol received: {}", coinSymbol);

        // Set shared model attributes early
        model.addAttribute("metrics", TrainingResult.builder().build());
        model.addAttribute("history", List.of());
        model.addAttribute("predictionForm", predictionForm);

        return djlTrainingService.predict(coinSymbol)  // Mono<PredictionResult>
                .doOnNext(prediction -> {
                    prediction.setCoinSymbol(coinSymbol); // optional redundancy
                    model.addAttribute("prediction", prediction);
                })
                .map(result -> "backtesting")
                .onErrorResume(e -> {
                    log.error("Prediction failed", e);
                    model.addAttribute("predictionError", "Prediction failed: " + e.getMessage());
                    return Mono.just("backtesting");
                });
    }
}
