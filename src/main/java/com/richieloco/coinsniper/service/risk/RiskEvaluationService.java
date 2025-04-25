package com.richieloco.coinsniper.service.risk;

import com.richieloco.coinsniper.service.risk.ai.CoinRiskAssessor;
import com.richieloco.coinsniper.service.risk.ai.ExchangeRiskAssessor;
import com.richieloco.coinsniper.service.risk.ai.context.CoinRiskContext;
import com.richieloco.coinsniper.service.risk.ai.context.ExchangeRiskContext;
import org.springframework.stereotype.Service;

@Service
public class RiskEvaluationService {

    private final ExchangeRiskAssessor exchangeRiskAssessor;
    private final CoinRiskAssessor coinRiskAssessor;

    public RiskEvaluationService(ExchangeRiskAssessor exchangeRiskAssessor,
                                 CoinRiskAssessor coinRiskAssessor) {
        this.exchangeRiskAssessor = exchangeRiskAssessor;
        this.coinRiskAssessor = coinRiskAssessor;
    }

    public double assessExchangeRisk(String from, String to, double volatility, double liquidityDiff, double feeDiff) {
        var context = new ExchangeRiskContext(from, to, volatility, liquidityDiff, feeDiff);
        return exchangeRiskAssessor.assessRisk(context);
    }

    public double assessCoinRisk(String coinA, String coinB, double volatility, double correlation, double volumeDiff) {
        var context = new CoinRiskContext(coinA, coinB, volatility, correlation, volumeDiff);
        return coinRiskAssessor.assessRisk(context);
    }
}
