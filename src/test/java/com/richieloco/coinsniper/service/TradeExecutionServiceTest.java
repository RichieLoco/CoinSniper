package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.entity.RiskLevel;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class TradeExecutionServiceTest {

    private ExchangeAssessor assessor;
    private TradeDecisionRepository repo;
    private CoinSniperConfig config;

    @BeforeEach
    public void setup() {
        assessor = mock(ExchangeAssessor.class);
        repo = mock(TradeDecisionRepository.class);
        config = new CoinSniperConfig();
        config.setSupported(new CoinSniperConfig.Supported());
        config.getSupported().setExchanges(List.of("Binance"));
        config.getSupported().setStableCoins(List.of("USDT"));
    }

    @Test
    public void testEvaluateAndTrade_successfulAssessment() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .id(UUID.randomUUID().toString())
                .coinSymbol("XYZ")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        ExchangeAssessmentRecord assessment = ExchangeAssessmentRecord.builder()
                .exchange("Binance")
                .coinListing("XYZUSDT")
                .overallRiskScore(3)
                .liquidity(RiskLevel.Medium)
                .tradingFees(RiskLevel.Low)
                .tradingVolume(RiskLevel.Medium)
                .assessedAt(Instant.now())
                .contextType("Exchange")
                .build();

        when(assessor.assess(any(ExchangeSelectorContext.class))).thenReturn(Mono.just(assessment));
        when(repo.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        TradeExecutionService service = new TradeExecutionService(assessor, repo, config);

        StepVerifier.create(service.evaluateAndTrade(announcement))
                .expectNextMatches(trade -> trade.getExchange().equals("Binance") && trade.isTradeExecuted())
                .verifyComplete();
    }
}
