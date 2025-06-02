package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.service.AnnouncementCallingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.mockito.Mockito.*;

public class AnnouncementCallControllerTest {

    @Mock
    private AnnouncementCallingService announcementCallingService;

    private AnnouncementCallController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new AnnouncementCallController(announcementCallingService);
    }

    @Test
    public void callBinance_shouldReturnAnnouncements() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .title("XYZ Listing")
                .coinSymbol("XYZ")
                .announcedAt(Instant.now())
                .build();

        when(announcementCallingService.callBinanceAnnouncements()).thenReturn(Flux.just(announcement));

        StepVerifier.create(controller.callBinance())
                .expectNext(announcement)
                .verifyComplete();
    }

    @Test
    public void callBinance_shouldHandleEmpty() {
        when(announcementCallingService.callBinanceAnnouncements()).thenReturn(Flux.empty());

        StepVerifier.create(controller.callBinance())
                .verifyComplete();
    }
}
