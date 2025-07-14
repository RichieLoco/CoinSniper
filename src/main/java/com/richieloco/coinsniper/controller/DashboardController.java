package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.DashboardConfig;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import com.richieloco.coinsniper.repository.ErrorResponseRepository;
import com.richieloco.coinsniper.repository.ExchangeAssessmentRepository;
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
    private final CoinAnnouncementRepository coinAnnouncementRepository;
    private final ExchangeAssessmentRepository exchangeAssessmentRepository;
    private final ErrorResponseRepository errorResponseRepository;
    private final DashboardConfig dashboardConfig;

    @GetMapping("/dashboard")
    public Mono<String> viewDashboard(Model model) {
        Mono<?> tradesMono = tradeDecisionRepository.findAll().collectList();
        Mono<?> announcementsMono = coinAnnouncementRepository.findAll().collectList();
        Mono<?> assessmentsMono = exchangeAssessmentRepository.findAll().collectList();
        Mono<?> errorsMono = errorResponseRepository.findAll().collectList();

        return Mono.zip(tradesMono, announcementsMono, assessmentsMono, errorsMono)
                .doOnNext(tuple -> {
                    model.addAttribute("trades", tuple.getT1());
                    model.addAttribute("announcements", tuple.getT2());
                    model.addAttribute("assessments", tuple.getT3());
                    model.addAttribute("errors", tuple.getT4());
                    model.addAttribute("dashboard", dashboardConfig);
                })
                .thenReturn("dashboard");
    }
}

