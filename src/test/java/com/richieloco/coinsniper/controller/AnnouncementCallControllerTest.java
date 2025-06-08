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

@SpringBootTest
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
}
