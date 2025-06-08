package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.AnnouncementPollingConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig.Api.Binance.Announcement;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.time.Duration;

class AnnouncementPollingSchedulerTest {

    @Test
    void pollingStarts_whenEnabled() {
        // Setup config with fast interval
        var pollingConfig = new AnnouncementPollingConfig();
        pollingConfig.setEnabled(true);
        pollingConfig.setIntervalSeconds(1);

        // Mock the AnnouncementCallingService
        var callingService = mock(AnnouncementCallingService.class);
        when(callingService.callBinanceAnnouncements(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

        // Setup config stubs
        var coinSniperConfig = mock(CoinSniperConfig.class);
        var api = mock(CoinSniperConfig.Api.class);
        var binance = mock(CoinSniperConfig.Api.Binance.class);
        var announcement = new Announcement();
        announcement.setType(1);
        announcement.setPageNo(1);
        announcement.setPageSize(10);

        when(coinSniperConfig.getApi()).thenReturn(api);
        when(api.getBinance()).thenReturn(binance);
        when(binance.getAnnouncement()).thenReturn(announcement);
        // Create scheduler manually (not as Spring bean)
        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);

        // Run the scheduler logic
        scheduler.startPolling();

        // Await and verify interaction with the mocked service
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(callingService, atLeastOnce()).callBinanceAnnouncements(1, 1, 10)
        );
    }

    @Test
    void pollingDoesNotStart_whenDisabled() {
        var pollingConfig = new AnnouncementPollingConfig();
        pollingConfig.setEnabled(false);

        var callingService = mock(AnnouncementCallingService.class);
        var coinSniperConfig = mock(CoinSniperConfig.class);

        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);
        scheduler.startPolling();

        verifyNoInteractions(callingService);
    }
}
