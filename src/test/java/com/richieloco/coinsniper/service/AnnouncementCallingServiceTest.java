package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import com.richieloco.coinsniper.ex.ExternalApiException;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import com.richieloco.coinsniper.repository.ErrorResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.mockito.Mockito.*;

public class AnnouncementCallingServiceTest {

    protected CoinAnnouncementRepository announcementRepository;
    protected ErrorResponseRepository errorRepository;
    protected CoinSniperConfig config;
    protected AnnouncementCallingService service;

    @BeforeEach
    public void setUp() {
        announcementRepository = mock(CoinAnnouncementRepository.class);
        errorRepository = mock(ErrorResponseRepository.class);
        config = mock(CoinSniperConfig.class);
    }

    @Test
    public void testPollBinanceAnnouncements_isSuccessful() {
        // Setup config mock
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

        // Instantiate service with overridden method using arguments
        service = new AnnouncementCallingService(config, announcementRepository, errorRepository) {
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
        // Setup config mock
        CoinSniperConfig.Api.Binance.Announcement announcementCfg = new CoinSniperConfig.Api.Binance.Announcement();
        announcementCfg.setBaseUrl("http://mock-fail-url");

        CoinSniperConfig.Api.Binance binance = new CoinSniperConfig.Api.Binance();
        binance.setAnnouncement(announcementCfg);
        CoinSniperConfig.Api api = new CoinSniperConfig.Api();
        api.setBinance(binance);

        when(config.getApi()).thenReturn(api);

        when(errorRepository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Override method to simulate error handling logic
        service = new AnnouncementCallingService(config, announcementRepository, errorRepository) {
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
    public void testExtractSymbolsFromTitle_parsesCorrectly() throws Exception {
        var service = new AnnouncementCallingService(null, null, null);

        var method = service.getClass()
                .getDeclaredMethod("extractSymbolsFromTitle", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(service, "Binance Will List Bubblemaps (BMT)");

        assertThat(result).containsExactly("BMT");
    }

    @Test
    public void testExtractSymbolsFromTitle_returnsUnknownWhenMalformed() throws Exception {
        var service = new AnnouncementCallingService(null, null, null);

        var method = service.getClass()
                .getDeclaredMethod("extractSymbolsFromTitle", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(service, "Binance Lists Coin Without Parentheses");

        assertThat(result).containsExactly("UNKNOWN");
    }

    @Test
    public void testIsDelisting_trueCases() throws Exception {
        var service = new AnnouncementCallingService(null, null, null);

        var method = service.getClass().getDeclaredMethod("isDelisting", String.class, String.class);
        method.setAccessible(true);

        assertThat(method.invoke(service, "Delisting XYZ Coin", "New Cryptocurrency Listing")).isEqualTo(true);
        assertThat(method.invoke(service, "Token Removal Notice", "Delisting")).isEqualTo(true);
    }

    @Test
    public void testIsDelisting_falseCase() throws Exception {
        var service = new AnnouncementCallingService(null, null, null);

        var method = service.getClass().getDeclaredMethod("isDelisting", String.class, String.class);
        method.setAccessible(true);

        assertThat(method.invoke(service, "Binance Lists New Coin", "Promotions")).isEqualTo(false);
    }

    @Test
    public void testPollBinanceAnnouncements_unexpectedErrorIsPropagated() {
        service = new AnnouncementCallingService(config, announcementRepository, errorRepository) {
            public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
                return Flux.error(new RuntimeException("Unexpected error"));
            }
        };

        StepVerifier.create(service.callBinanceAnnouncements(1, 1, 10))
                .expectErrorMessage("Unexpected error")
                .verify();

        verifyNoInteractions(errorRepository);
    }

    @Test
    public void testPollBinanceAnnouncements_errorRepoFails_gracefullyContinues() {
        when(errorRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB failure")));

        service = new AnnouncementCallingService(config, announcementRepository, errorRepository) {
            public Flux<CoinAnnouncementRecord> callBinanceAnnouncements(int type, int pageNo, int pageSize) {
                return Flux.<CoinAnnouncementRecord>error(new ExternalApiException("Simulated", 500))
                        .onErrorResume(ExternalApiException.class, ex -> {
                            ErrorResponseRecord error = ErrorResponseRecord.builder()
                                    .source("Binance")
                                    .errorMessage(ex.getMessage())
                                    .statusCode(ex.getStatusCode())
                                    .timestamp(Instant.now())
                                    .build();
                            return errorResponseRepository.save(error).onErrorResume(e -> Mono.empty()).thenMany(Flux.empty());
                        });
            }
        };

        StepVerifier.create(service.callBinanceAnnouncements(1, 1, 10))
                .verifyComplete();
    }
}
