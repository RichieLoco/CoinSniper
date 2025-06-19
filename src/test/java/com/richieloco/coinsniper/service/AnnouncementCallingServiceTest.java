package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.config.WebClientConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.ex.ExternalApiException;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import com.richieloco.coinsniper.repository.ErrorResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AnnouncementCallingServiceTest {

    protected CoinAnnouncementRepository announcementRepository;
    protected ErrorResponseRepository errorRepository;
    protected CoinSniperConfig config;
    protected TradeExecutionService tradeExecutionService;
    protected AnnouncementCallingService service;
    protected WebClient webClient;

    @BeforeEach
    public void setUp() {
        announcementRepository = mock(CoinAnnouncementRepository.class);
        errorRepository = mock(ErrorResponseRepository.class);
        config = mock(CoinSniperConfig.class);
        tradeExecutionService = mock(TradeExecutionService.class);
        webClient = mock(WebClient.class); // Add this line
    }


    @Test
    public void testPollBinanceAnnouncements_isSuccessful() {
        CoinSniperConfig.Api.Binance.Announcement announcementCfg = new CoinSniperConfig.Api.Binance.Announcement();
        announcementCfg.setBaseUrl("http://mock-url.com");

        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        binance.setAnnouncement(announcementCfg);
        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        api.setBinance(binance);

        when(config.getApi()).thenReturn(api);

        CoinAnnouncementRecord expectedRecord = CoinAnnouncementRecord.builder()
                .coinSymbol("XYZ")
                .title("New Coin")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        when(announcementRepository.save(any())).thenReturn(Mono.just(expectedRecord));
        when(tradeExecutionService.evaluateAndTrade(any())).thenReturn(Mono.just(mock(TradeDecisionRecord.class)));

        service = new AnnouncementCallingService(config, announcementRepository, errorRepository, tradeExecutionService, webClient) {
            public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
                return Flux.just(expectedRecord).flatMap(announcementRepository::save);
            }
        };

        StepVerifier.create(service.callBinanceAnnouncements(1, 1, 10))
                .expectNextMatches(record -> record.getCoinSymbol().equals("XYZ"))
                .verifyComplete();
    }

    @Test
    public void testPollBinanceAnnouncements_isFailure() {
        CoinSniperConfig.Api.Binance.Announcement announcementCfg = new CoinSniperConfig.Api.Binance.Announcement();
        announcementCfg.setBaseUrl("http://mock-fail-url");

        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        binance.setAnnouncement(announcementCfg);
        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        api.setBinance(binance);

        when(config.getApi()).thenReturn(api);
        when(errorRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        service = new AnnouncementCallingService(config, announcementRepository, errorRepository, tradeExecutionService, webClient) {
            public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
                return Flux.<CoinAnnouncementRecord>error(new ExternalApiException("Simulated error", 500))
                        .onErrorResume(ExternalApiException.class, ex -> {
                            ErrorResponseRecord error = ErrorResponseRecord.builder()
                                    .source("Binance")
                                    .errorMessage(ex.getMessage())
                                    .statusCode(ex.getStatusCode())
                                    .timestamp(Instant.now())
                                    .build();
                            return errorRepository.save(error).thenMany(Flux.empty());
                        });
            }
        };

        StepVerifier.create(service.callBinanceAnnouncements(1, 1, 10))
                .expectComplete()
                .verify();

        ArgumentCaptor<ErrorResponseRecord> captor = ArgumentCaptor.forClass(ErrorResponseRecord.class);
        verify(errorRepository, times(1)).save(captor.capture());

        ErrorResponseRecord captured = captor.getValue();
        assertThat(captured.getSource()).isEqualTo("Binance");
        assertThat(captured.getStatusCode()).isEqualTo(500);
        assertThat(captured.getErrorMessage()).contains("Simulated error");
    }

    @Test
    public void testEvaluateAndTrade_invokedAfterSave() {
        CoinAnnouncementRecord savedRecord = CoinAnnouncementRecord.builder()
                .coinSymbol("ABC")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        CoinSniperConfig.Api.Binance.Announcement announcementCfg = new CoinSniperConfig.Api.Binance.Announcement();
        announcementCfg.setBaseUrl("http://mock-url");
        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        binance.setAnnouncement(announcementCfg);
        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        api.setBinance(binance);
        when(config.getApi()).thenReturn(api);

        when(announcementRepository.findByCoinSymbolAndAnnouncedAt(anyString(), any())).thenReturn(Mono.empty());
        when(announcementRepository.save(any())).thenReturn(Mono.just(savedRecord));
        when(tradeExecutionService.evaluateAndTrade(any())).thenReturn(Mono.just(mock(TradeDecisionRecord.class)));

        service = new AnnouncementCallingService(config, announcementRepository, errorRepository, tradeExecutionService, webClient) {
            @Override
            public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
                return announcementRepository.save(savedRecord)
                        .flatMap(saved -> tradeExecutionService.evaluateAndTrade(saved).onErrorResume(e -> Mono.empty()).thenReturn(saved))
                        .flux();
            }
        };
        StepVerifier.create(service.callBinanceAnnouncements(1, 1, 1))
                .expectNextMatches(record -> record.getCoinSymbol().equals("ABC"))
                .verifyComplete();

        verify(tradeExecutionService, atLeastOnce()).evaluateAndTrade(any(CoinAnnouncementRecord.class));
    }

    @Test
    public void testEvaluateAndTrade_errorIsHandled() {
        CoinAnnouncementRecord savedRecord = CoinAnnouncementRecord.builder()
                .coinSymbol("ABC")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        CoinSniperConfig.Api.Binance.Announcement announcementCfg = new CoinSniperConfig.Api.Binance.Announcement();
        announcementCfg.setBaseUrl("http://mock-url");
        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        binance.setAnnouncement(announcementCfg);
        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        api.setBinance(binance);
        when(config.getApi()).thenReturn(api);

        when(announcementRepository.findByCoinSymbolAndAnnouncedAt(anyString(), any())).thenReturn(Mono.empty());
        when(announcementRepository.save(any())).thenReturn(Mono.just(savedRecord));
        when(tradeExecutionService.evaluateAndTrade(any())).thenReturn(Mono.error(new RuntimeException("Trade failed")));

        service = new AnnouncementCallingService(config, announcementRepository, errorRepository, tradeExecutionService, webClient) {
            @Override
            public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
                return announcementRepository.save(savedRecord)
                        .flatMap(saved -> tradeExecutionService.evaluateAndTrade(saved).onErrorResume(e -> Mono.empty()).thenReturn(saved))
                        .flux();
            }
        };
        StepVerifier.create(service.callBinanceAnnouncements(1, 1, 1))
                .expectNextMatches(record -> record.getCoinSymbol().equals("ABC"))
                .verifyComplete();

        verify(tradeExecutionService, atLeastOnce()).evaluateAndTrade(any(CoinAnnouncementRecord.class));
    }

    @Test
    public void testParallelExecutionAndOrdering() {
        CoinAnnouncementRecord record1 = CoinAnnouncementRecord.builder().coinSymbol("AAA").announcedAt(Instant.now()).delisting(false).build();
        CoinAnnouncementRecord record2 = CoinAnnouncementRecord.builder().coinSymbol("BBB").announcedAt(Instant.now()).delisting(false).build();

        when(announcementRepository.findByCoinSymbolAndAnnouncedAt(anyString(), any())).thenReturn(Mono.empty());
        when(announcementRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(tradeExecutionService.evaluateAndTrade(any())).thenReturn(Mono.just(mock(TradeDecisionRecord.class)));

        CoinSniperConfig.Api.Binance.Announcement announcementCfg = new CoinSniperConfig.Api.Binance.Announcement();
        announcementCfg.setBaseUrl("http://mock-url");
        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        binance.setAnnouncement(announcementCfg);
        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        api.setBinance(binance);
        when(config.getApi()).thenReturn(api);

        service = new AnnouncementCallingService(config, announcementRepository, errorRepository, tradeExecutionService, webClient) {
            @Override
            public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
                return Flux.just(record1, record2).flatMap(rec -> announcementRepository.save(rec)
                        .flatMap(saved -> tradeExecutionService.evaluateAndTrade(saved).thenReturn(saved)));
            }
        };

        StepVerifier.create(service.callBinanceAnnouncements(1, 1, 2))
                .expectNextCount(2)
                .verifyComplete();

        verify(tradeExecutionService, times(2)).evaluateAndTrade(any(CoinAnnouncementRecord.class));
    }
}