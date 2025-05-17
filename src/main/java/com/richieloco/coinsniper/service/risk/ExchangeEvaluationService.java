package com.richieloco.coinsniper.service.risk;

import com.richieloco.coinsniper.entity.on.Risk;
import com.richieloco.coinsniper.service.risk.ai.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.ai.context.ExchangeSelectorContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class ExchangeEvaluationService {

    private final ExchangeAssessor exchangeRiskAssessor;

    public ExchangeEvaluationService(ExchangeAssessor exchangeRiskAssessor) {
        this.exchangeRiskAssessor = exchangeRiskAssessor;
    }

    public Mono<Risk> assessExchangeRisk(String from, String to, double volatility, double liquidityDiff, double feeDiff) {
        var context = new ExchangeSelectorContext(from, to, volatility, liquidityDiff, feeDiff);
        return exchangeRiskAssessor.assess(context);
    }
}
