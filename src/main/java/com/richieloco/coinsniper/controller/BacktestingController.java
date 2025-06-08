package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.DJLTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class BacktestingController {

    private final TradeDecisionRepository tradeDecisionRepository;
    private final DJLTrainingService djlTrainingService;

    @GetMapping("/backtesting")
    public Mono<String> backtesting(Model model) {
        return tradeDecisionRepository.findAll()
                .collectList()
                .doOnNext(history -> {
                    try {
                        djlTrainingService.train(history);
                        djlTrainingService.logToFile(history);
                    } catch (Exception e) {
                        // log and swallow exception to prevent 500 error
                        System.err.println("Training/logging failed: " + e.getMessage());
                    }
                })
                .map(history -> {
                    model.addAttribute("history", history);
                    return "backtesting";
                });
    }
}
