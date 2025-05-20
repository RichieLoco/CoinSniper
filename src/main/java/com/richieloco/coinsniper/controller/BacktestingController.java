package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.DJLTrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class BacktestingController {

    private final TradeDecisionRepository tradeDecisionRepository;
    private final DJLTrainingService djlTrainingService;

    @GetMapping("/backtesting")
    public String backtesting(Model model) {
        return tradeDecisionRepository.findAll()
                .collectList()
                .doOnNext(djlTrainingService::train)
                .doOnNext(djlTrainingService::logToFile)
                .map(history -> {
                    model.addAttribute("history", history);
                    return "backtesting";
                })
                .block();
    }
}
