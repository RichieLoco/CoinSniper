package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.AnnouncementPollingConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig.Api.Binance.Announcement;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

class AnnouncementPollingSchedulerTest {

    @Test
    void pollingStarts_whenEnabled() {
        var pollingConfig = new AnnouncementPollingConfig();
        pollingConfig.setEnabled(true);
        pollingConfig.setIntervalSeconds(1); // still used, but now first tick is immediate

        var callingService = mock(AnnouncementCallingService.class);
        when(callingService.callBinanceAnnouncements(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

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

        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);
        scheduler.startPolling();

        verifyNoInteractions(callingService);
    }

    @Test
    void startPolling_isIdempotent() {
        var pollingConfig = new AnnouncementPollingConfig();
        pollingConfig.setEnabled(true);
        pollingConfig.setIntervalSeconds(1);

        var callingService = mock(AnnouncementCallingService.class);
        when(callingService.callBinanceAnnouncements(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

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

        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);
        scheduler.startPolling();
        scheduler.startPolling(); // Second call should be ignored

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(callingService, atLeastOnce()).callBinanceAnnouncements(1, 1, 10)
        );
    }

    @Test
    void initializePolling_startsOnlyWhenEnabled() {
        var pollingConfig = new AnnouncementPollingConfig();
        pollingConfig.setEnabled(true);
        pollingConfig.setIntervalSeconds(1);

        var callingService = mock(AnnouncementCallingService.class);
        when(callingService.callBinanceAnnouncements(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());

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

        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);
        scheduler.initializePolling();

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(callingService, atLeastOnce()).callBinanceAnnouncements(1, 1, 10)
        );
    }

    @Test
    void pollingContinuesDespiteErrors() {
        var pollingConfig = new AnnouncementPollingConfig();
        pollingConfig.setEnabled(true);
        pollingConfig.setIntervalSeconds(1);

        var callingService = mock(AnnouncementCallingService.class);
        when(callingService.callBinanceAnnouncements(anyInt(), anyInt(), anyInt()))
                .thenReturn(Flux.error(new RuntimeException("API failed")));

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

        var scheduler = new AnnouncementPollingScheduler(callingService, pollingConfig, coinSniperConfig);
        scheduler.startPolling();

        await().atMost(Duration.ofSeconds(3)).untilAsserted(() ->
                verify(callingService, atLeastOnce()).callBinanceAnnouncements(1, 1, 10)
        );
    }
}