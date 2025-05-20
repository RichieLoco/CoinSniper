package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final TradeDecisionRepository tradeDecisionRepository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("trades", tradeDecisionRepository.findAll());
        return "dashboard";
    }
}
