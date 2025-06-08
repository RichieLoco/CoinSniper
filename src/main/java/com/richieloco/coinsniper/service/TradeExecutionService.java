package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TradeExecutionService {

    private final ExchangeAssessor exchangeAssessor;
    private final TradeDecisionRepository repository;
    private final CoinSniperConfig config;

    public Mono<TradeDecisionRecord> evaluateAndTrade(CoinAnnouncementRecord announcement) {
        ExchangeSelectorContext context =
                ExchangeSelectorContext.from(config, announcement.getCoinSymbol());

        return exchangeAssessor.assess(context)
                .map(assessment -> TradeDecisionRecord.builder()
                        .coinSymbol(announcement.getCoinSymbol())
                        .exchange(assessment.getExchange())
                        .riskScore(assessment.getOverallRiskScore())
                        .tradeExecuted(assessment.getOverallRiskScore() < 5.0) // TODO: Simplified logic
                        .timestamp(Instant.now())
                        .build())
                .flatMap(repository::save);
    }
}
