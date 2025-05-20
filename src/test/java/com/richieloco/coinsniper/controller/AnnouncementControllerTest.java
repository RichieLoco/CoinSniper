package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.service.AnnouncementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class AnnouncementControllerTest {

    @Mock
    private AnnouncementService announcementService;

    private AnnouncementController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new AnnouncementController(announcementService);
    }

    @Test
    public void pollBinance_shouldReturnAnnouncements() {
        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .id(UUID.randomUUID().toString())
                .title("XYZ Listing")
                .coinSymbol("XYZ")
                .announcedAt(Instant.now())
                .build();

        when(announcementService.pollBinanceAnnouncements()).thenReturn(Flux.just(announcement));

        StepVerifier.create(controller.pollBinance())
                .expectNext(announcement)
                .verifyComplete();
    }

    @Test
    public void pollBinance_shouldHandleEmpty() {
        when(announcementService.pollBinanceAnnouncements()).thenReturn(Flux.empty());

        StepVerifier.create(controller.pollBinance())
                .verifyComplete();
    }
}
