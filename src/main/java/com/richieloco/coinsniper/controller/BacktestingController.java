package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.dto.PredictionForm;
import com.richieloco.coinsniper.dto.PredictionResult;
import com.richieloco.coinsniper.dto.TrainingResult;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
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
                .map(history -> {
                    model.addAttribute("history", history);
                    model.addAttribute("metrics", djlTrainingService.getLastTrainingResult());
                    model.addAttribute("predictionForm", new PredictionForm());
                    return "backtesting";
                });
    }

    @PostMapping("/backtesting/train")
    public Mono<String> train(Model model) {
        return tradeDecisionRepository.findAll()
                .collectList()
                .flatMap(history -> djlTrainingService.trainReactive(history)
                        .map(trainRes -> {
                            model.addAttribute("history", history);
                            model.addAttribute("metrics", trainRes);
                            model.addAttribute("predictionForm", new PredictionForm());
                            model.addAttribute("trainMessage", "Training completed and model updated.");
                            return "backtesting";
                        })
                        .onErrorResume(ex -> {
                            log.error("Training failed", ex);
                            model.addAttribute("history", history);
                            model.addAttribute("metrics", TrainingResult.builder()
                                    .modelSummary("Training failed: " + ex.getMessage())
                                    .build());
                            model.addAttribute("predictionForm", new PredictionForm());
                            model.addAttribute("trainError", "Training failed: " + ex.getMessage());
                            return Mono.just("backtesting");
                        })
                );
    }

    @PostMapping("/backtesting/predict")
    public Mono<String> predict(@ModelAttribute PredictionForm predictionForm, Model model) {
        String coinSymbol = predictionForm.getCoinSymbol();
        log.debug("Coin Symbol received: {}", coinSymbol);

        Mono<List<TradeDecisionRecord>> historyMono = tradeDecisionRepository.findAll().collectList();
        Mono<PredictionResult> predictionMono = djlTrainingService.predict(coinSymbol);

        return Mono.zip(historyMono, predictionMono)
                .map(tuple -> {
                    List<TradeDecisionRecord> history = tuple.getT1();
                    PredictionResult prediction = tuple.getT2();

                    model.addAttribute("history", history);
                    model.addAttribute("metrics", djlTrainingService.getLastTrainingResult());
                    model.addAttribute("predictionForm", predictionForm);
                    model.addAttribute("prediction", prediction);
                    return "backtesting";
                })
                .onErrorResume(ex -> {
                    log.error("Prediction failed", ex);
                    return tradeDecisionRepository.findAll().collectList()
                            .map(history -> {
                                model.addAttribute("history", history);
                                model.addAttribute("metrics", djlTrainingService.getLastTrainingResult());
                                model.addAttribute("predictionForm", predictionForm);
                                model.addAttribute("predictionError", "Prediction failed: " + ex.getMessage());
                                return "backtesting";
                            });
                });
    }
}
