package com.richieloco.coinsniper.handler;

import com.richieloco.coinsniper.entity.on.Risk;
import com.richieloco.coinsniper.service.risk.ExchangeEvaluationService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class ExchangeHandler {

    private final ExchangeEvaluationService exchangeEvaluationService;

    public ExchangeHandler(ExchangeEvaluationService exchangeEvaluationService) {
        this.exchangeEvaluationService = exchangeEvaluationService;
    }

    public Mono<ServerResponse> exchangeRisk(ServerRequest request) {
        try {
            String from = request.queryParam("from").orElseThrow(() -> new IllegalArgumentException("Missing 'from' parameter"));
            String to = request.queryParam("to").orElseThrow(() -> new IllegalArgumentException("Missing 'to' parameter"));
            double volatility = Double.parseDouble(request.queryParam("volatility").orElseThrow(() -> new IllegalArgumentException("Missing 'volatility' parameter")));
            double liquidity = Double.parseDouble(request.queryParam("liquidity").orElseThrow(() -> new IllegalArgumentException("Missing 'liquidity' parameter")));
            double fees = Double.parseDouble(request.queryParam("fees").orElseThrow(() -> new IllegalArgumentException("Missing 'fees' parameter")));

            return exchangeEvaluationService
                    .assessExchangeRisk(from, to, volatility, liquidity, fees)
                    .flatMap(risk -> ServerResponse.ok().bodyValue(risk));
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest().bodyValue(Map.of("error", e.getMessage()));
        }
    }

    public Mono<ServerResponse> coinRisk(ServerRequest request) {
        try {
            String coinA = request.queryParam("coinA").orElseThrow(() -> new IllegalArgumentException("Missing 'coinA' parameter"));
            String coinB = request.queryParam("coinB").orElseThrow(() -> new IllegalArgumentException("Missing 'coinB' parameter"));
            double volatility = Double.parseDouble(request.queryParam("volatility").orElseThrow(() -> new IllegalArgumentException("Missing 'volatility' parameter")));
            double correlation = Double.parseDouble(request.queryParam("correlation").orElseThrow(() -> new IllegalArgumentException("Missing 'correlation' parameter")));
            double volumeDiff = Double.parseDouble(request.queryParam("volumeDiff").orElseThrow(() -> new IllegalArgumentException("Missing 'volumeDiff' parameter")));

            Mono<Risk> risk = exchangeEvaluationService.assessCoinRisk(coinA, coinB, volatility, correlation, volumeDiff);
            return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(risk, Risk.class);
        } catch (IllegalArgumentException e) {
            return ServerResponse.badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("error", e.getMessage()));
        }
    }

}
