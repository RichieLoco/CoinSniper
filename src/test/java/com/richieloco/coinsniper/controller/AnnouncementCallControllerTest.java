package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.config.CoinSniperMockTestConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.service.AnnouncementCallingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.*;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import(CoinSniperMockTestConfig.class)
public class AnnouncementCallControllerTest {

    @Autowired
    private CoinSniperConfig coinSniperConfig;

    @Test
    public void callBinance_shouldReturnAnnouncements() {
        // 👇 Manual mock
        AnnouncementCallingService announcementCallingService = mock(AnnouncementCallingService.class);

        CoinAnnouncementRecord mockRecord = CoinAnnouncementRecord.builder()
                .title("XYZ Listing")
                .coinSymbol("XYZ")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        when(announcementCallingService.callBinanceAnnouncements(1, 1, 10))
                .thenReturn(Flux.just(mockRecord));

        // 👇 Manual injection — no @Autowired
        AnnouncementCallController controller = new AnnouncementCallController(announcementCallingService, coinSniperConfig);

        StepVerifier.create(controller.callBinance(1, 1, 10))
                .expectNextMatches(record ->
                        "XYZ Listing".equals(record.getTitle()) &&
                                "XYZ".equals(record.getCoinSymbol()) &&
                                !record.isDelisting())
                .verifyComplete();
    }

    @Test
    public void callBinance_shouldUseDefaultsFromConfigWhenParamsAreNull() {
        // 👇 Set expected defaults
        var defaults = coinSniperConfig.getApi().getBinance().getAnnouncement();
        int defaultType = defaults.getType();
        int defaultPageNo = defaults.getPageNo();
        int defaultPageSize = defaults.getPageSize();

        AnnouncementCallingService service = mock(AnnouncementCallingService.class);

        when(service.callBinanceAnnouncements(defaultType, defaultPageNo, defaultPageSize))
                .thenReturn(Flux.empty());

        AnnouncementCallController controller = new AnnouncementCallController(service, coinSniperConfig);

        StepVerifier.create(controller.callBinance(null, null, null))
                .verifyComplete();

        verify(service).callBinanceAnnouncements(defaultType, defaultPageNo, defaultPageSize);
    }

    @Test
    public void callBinance_shouldReturnMultipleAnnouncements() {
        AnnouncementCallingService service = mock(AnnouncementCallingService.class);

        CoinAnnouncementRecord one = CoinAnnouncementRecord.builder().coinSymbol("AAA").title("AAA listing").build();
        CoinAnnouncementRecord two = CoinAnnouncementRecord.builder().coinSymbol("BBB").title("BBB listing").build();

        when(service.callBinanceAnnouncements(1, 1, 10)).thenReturn(Flux.just(one, two));

        AnnouncementCallController controller = new AnnouncementCallController(service, coinSniperConfig);

        StepVerifier.create(controller.callBinance(1, 1, 10))
                .expectNext(one)
                .expectNext(two)
                .verifyComplete();
    }

    @Test
    public void callBinance_shouldPropagateErrors() {
        AnnouncementCallingService service = mock(AnnouncementCallingService.class);

        when(service.callBinanceAnnouncements(1, 1, 10))
                .thenReturn(Flux.error(new RuntimeException("Binance failure")));

        AnnouncementCallController controller = new AnnouncementCallController(service, coinSniperConfig);

        StepVerifier.create(controller.callBinance(1, 1, 10))
                .expectErrorMessage("Binance failure")
                .verify();
    }
}
