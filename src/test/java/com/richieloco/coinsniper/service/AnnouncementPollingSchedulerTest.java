package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.AnnouncementPollingConfig;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;


import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.time.Duration;

class AnnouncementPollingSchedulerTest {

    @Test
    void pollingStarts_whenEnabled() {
        var config = new AnnouncementPollingConfig();
        config.setEnabled(true);
        config.setIntervalSeconds(1); // fast polling for test

        var announcementService = mock(AnnouncementCallingService.class);
        when(announcementService.callBinanceAnnouncements())
                .thenReturn(Flux.empty());

        var scheduler = new AnnouncementPollingScheduler(announcementService, config);
        scheduler.startPolling();

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(announcementService, atLeastOnce()).callBinanceAnnouncements()
        );
    }

    @Test
    void pollingDoesNotStart_whenDisabled() {
        var config = new AnnouncementPollingConfig();
        config.setEnabled(false);

        var announcementService = mock(AnnouncementCallingService.class);

        var scheduler = new AnnouncementPollingScheduler(announcementService, config);
        scheduler.startPolling();

        verifyNoInteractions(announcementService);
    }
}
