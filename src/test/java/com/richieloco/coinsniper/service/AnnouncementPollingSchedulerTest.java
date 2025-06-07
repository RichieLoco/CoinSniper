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
        var pollingConfig = new AnnouncementPollingConfig();
        pollingConfig.setEnabled(true);
        pollingConfig.setIntervalSeconds(1); // fast polling for test

        var callingService = mock(AnnouncementCallingService.class);
        when(callingService.callBinanceAnnouncements(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

        var coinSniperConfig = mock(CoinSniperConfig.class);
        var announcementConfig = new Announcement();
        announcementConfig.setType(1);
        announcementConfig.setPageNo(1);
        announcementConfig.setPageSize(10);

        var binance = new CoinSniperConfig.Api.Binance();
        binance.setAnnouncement(announcementConfig);

        var api = new CoinSniperConfig.Api();
        api.setBinance(binance);

        when(coinSniperConfig.getApi()).thenReturn(api);

        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);
        scheduler.startPolling();

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

        // Full mock chain to avoid NPEs
        var announcement = new Announcement();
        announcement.setType(1);
        announcement.setPageNo(1);
        announcement.setPageSize(10);

        var binance = mock(CoinSniperConfig.Api.Binance.class);
        when(binance.getAnnouncement()).thenReturn(announcement);

        var api = mock(CoinSniperConfig.Api.class);
        when(api.getBinance()).thenReturn(binance);

        when(coinSniperConfig.getApi()).thenReturn(api);

        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);
        scheduler.startPolling();

        verifyNoInteractions(callingService);
    }

}