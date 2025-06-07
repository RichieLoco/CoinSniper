package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.config.CoinSniperMockTestConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.service.AnnouncementCallingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.*;

@SpringBootTest
@Import(CoinSniperMockTestConfig.class)
public class AnnouncementCallControllerTest {

    @Mock
    private AnnouncementCallingService announcementCallingService;

    @Autowired
    private CoinSniperConfig coinSniperConfig;

    @Autowired
    private AnnouncementCallController controller;

    @Test
    public void callBinance_shouldReturnAnnouncements() {
        CoinAnnouncementRecord mockRecord = CoinAnnouncementRecord.builder()
                .title("XYZ Listing")
                .coinSymbol("XYZ")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        when(announcementCallingService.callBinanceAnnouncements(1, 1, 10))
                .thenReturn(Flux.just(mockRecord));

        StepVerifier.create(controller.callBinance(1, 1, 10))
                .expectNextMatches(record -> "XYZ Listing".equals(record.getTitle()))
                .verifyComplete();
    }

    @Test
    public void callBinance_shouldHandleEmpty() {
        when(announcementCallingService.callBinanceAnnouncements(1, 1, 10))
                .thenReturn(Flux.empty());

        StepVerifier.create(controller.callBinance(1, 1, 10))
                .expectComplete()
                .verify();
    }
}