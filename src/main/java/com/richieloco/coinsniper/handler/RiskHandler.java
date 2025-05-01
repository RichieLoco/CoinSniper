package com.richieloco.coinsniper.handler;

import com.richieloco.coinsniper.service.risk.RiskEvaluationService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class RiskHandler {

    private final RiskEvaluationService riskEvaluationService;

    public RiskHandler(RiskEvaluationService riskEvaluationService) {
        this.riskEvaluationService = riskEvaluationService;
    }

    public Mono<ServerResponse> exchangeRisk(ServerRequest request) {
        String from = request.queryParam("from").orElseThrow();
        String to = request.queryParam("to").orElseThrow();
        double volatility = Double.parseDouble(request.queryParam("volatility").orElse("0"));
        double liquidity = Double.parseDouble(request.queryParam("liquidity").orElse("0"));
        double fees = Double.parseDouble(request.queryParam("fees").orElse("0"));

        double risk = riskEvaluationService.assessExchangeRisk(from, to, volatility, liquidity, fees);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(risk);
    }

    public Mono<ServerResponse> coinRisk(ServerRequest request) {
        String coinA = request.queryParam("coinA").orElseThrow();
        String coinB = request.queryParam("coinB").orElseThrow();
        double volatility = Double.parseDouble(request.queryParam("volatility").orElse("0"));
        double correlation = Double.parseDouble(request.queryParam("correlation").orElse("0"));
        double volumeDiff = Double.parseDouble(request.queryParam("volumeDiff").orElse("0"));

        double risk = riskEvaluationService.assessCoinRisk(coinA, coinB, volatility, correlation, volumeDiff);
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(risk);
    }
}
