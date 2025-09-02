package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TradeExecutionService {

    private final ExchangeAssessor exchangeAssessor;
    private final TradeDecisionRepository repository;
    private final CoinSniperConfig config;

    public Flux<TradeDecisionRecord> evaluateAndTrade(CoinAnnouncementRecord announcement) {
        if (announcement.isDelisting()) {
            return Flux.empty();
        }

        ExchangeSelectorContext context = ExchangeSelectorContext.from(config, announcement.getCoinSymbol());

        return exchangeAssessor.assess(context)
                .flatMapMany(assessments -> Flux.fromIterable(assessments)
                        .filter(assessment ->
                                assessment != null &&
                                        config.getSupported().getExchanges().contains(assessment.getExchange()))
                        .flatMap(assessment -> {
                            TradeDecisionRecord record = TradeDecisionRecord.builder()
                                    .coinSymbol(announcement.getCoinSymbol())
                                    .exchange(assessment.getExchange())
                                    .riskScore(mapRiskToNumeric(assessment.getOverallRiskScore()))
                                    .tradeExecuted(mapRiskToNumeric(assessment.getOverallRiskScore()) <= 5.0)
                                    .timestamp(Instant.now())
                                    .build();
                            return repository.save(record);
                        }));
    }

    private double mapRiskToNumeric(String risk) {
        return switch (risk.toLowerCase()) {
            case "low" -> 2;
            case "medium" -> 5;
            case "high" -> 8;
            default -> 10; // Unknown = high risk
        };
    }

}