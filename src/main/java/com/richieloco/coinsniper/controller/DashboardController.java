package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.DashboardConfig;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TradeDecisionRepository tradeDecisionRepository;
    private final DashboardConfig dashboardConfig;

    @GetMapping("/dashboard")
    public Mono<String> viewDashboard(Model model) {
        return tradeDecisionRepository.findAll().collectList()
                .doOnNext(trades -> {
                    model.addAttribute("trades", trades);
                    model.addAttribute("dashboard", dashboardConfig);
                })
                .thenReturn("dashboard");
    }
}
