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
    public void testEvaluateAndTrade_givenAssessment() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .coinSymbol("XYZ")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        ExchangeAssessmentRecord assessment = ExchangeAssessmentRecord.builder()
                .exchange("Binance")
                .coinListing("XYZUSDT")
                .overallRiskScore(3)
                .liquidity(RiskLevel.Medium.name())
                .tradingFees(RiskLevel.Low.name())
                .tradingVolume(RiskLevel.Medium.name())
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

    @Test
    public void testEvaluateAndTrade_unsupportedExchange_skipsTrade() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .coinSymbol("XYZ")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        ExchangeAssessmentRecord unsupportedAssessment = ExchangeAssessmentRecord.builder()
                .exchange("UnknownExchange")
                .coinListing("XYZUSDT")
                .overallRiskScore(3)
                .assessedAt(Instant.now())
                .contextType("Exchange")
                .build();

        when(assessor.assess(any())).thenReturn(Mono.just(unsupportedAssessment));

        TradeExecutionService service = new TradeExecutionService(assessor, repo, config);

        StepVerifier.create(service.evaluateAndTrade(announcement))
                .expectComplete()
                .verify();

        verify(repo, never()).save(any());
    }

    @Test
    public void testEvaluateAndTrade_delisting_skipsTrade() {
        CoinAnnouncementRecord delistingAnnouncement = CoinAnnouncementRecord.builder()
                .coinSymbol("ABC")
                .delisting(true)
                .announcedAt(Instant.now())
                .build();

        // Ensure a mock Mono is returned
        when(assessor.assess(any())).thenReturn(Mono.empty());

        TradeExecutionService service = new TradeExecutionService(assessor, repo, config);

        StepVerifier.create(service.evaluateAndTrade(delistingAnnouncement))
                .verifyComplete();

        verifyNoInteractions(repo);
    }

    @Test
    public void testEvaluateAndTrade_nullAssessment_skipsTrade() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .coinSymbol("DEF")
                .delisting(false)
                .announcedAt(Instant.now())
                .build();

        when(assessor.assess(any())).thenReturn(Mono.empty());

        TradeExecutionService service = new TradeExecutionService(assessor, repo, config);

        StepVerifier.create(service.evaluateAndTrade(announcement))
                .expectComplete()
                .verify();

        verify(repo, never()).save(any());
    }

    @Test
    public void testEvaluateAndTrade_assessmentFails_propagatesError() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .coinSymbol("FAIL")
                .delisting(false)
                .announcedAt(Instant.now())
                .build();

        when(assessor.assess(any())).thenReturn(Mono.error(new RuntimeException("Assessment error")));

        TradeExecutionService service = new TradeExecutionService(assessor, repo, config);

        StepVerifier.create(service.evaluateAndTrade(announcement))
                .expectErrorMessage("Assessment error")
                .verify();
    }
}
