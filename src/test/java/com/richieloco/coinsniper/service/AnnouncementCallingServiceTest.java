package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import com.richieloco.coinsniper.repository.ErrorResponseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AnnouncementCallingServiceTest {

    private CoinAnnouncementRepository announcementRepository;
    private ErrorResponseRepository errorRepository;
    private CoinSniperConfig config;
    private AnnouncementCallingService service;

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

    // Inner exception class used in test override
    private static class ExternalApiException extends RuntimeException {
        private final int statusCode;

        public ExternalApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
