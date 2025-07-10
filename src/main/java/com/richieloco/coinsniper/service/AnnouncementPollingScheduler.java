package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.AnnouncementPollingConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.richieloco.coinsniper.service.AnnouncementCallingService.UNKNOWN_COIN;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementPollingScheduler {

    private final AnnouncementCallingService service;
    private final AnnouncementPollingConfig config;
    private final CoinSniperConfig coinSniperConfig;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pollingTask;
    private final AtomicBoolean pollingActive = new AtomicBoolean(false);

    public void initializePolling() {
        if (config.isEnabled()) {
            startPolling();
        }
    }

    @PreDestroy
    public void shutdown() {
        stopPolling();
        executor.shutdownNow();
    }


    public synchronized void startPolling() {
        if (pollingActive.get()) {
            log.info("Polling already active.");
            return;
        }

        Runnable pollingRunnable = () -> {
            log.info("Polling Binance...");
            try {
                var announcementCfg = coinSniperConfig.getApi().getBinance().getAnnouncement();
                List<CoinAnnouncementRecord> records = service.callBinanceAnnouncements(
                                announcementCfg.getType(),
                                announcementCfg.getPageNo(),
                                announcementCfg.getPageSize()
                        )
                        .filter(record -> !UNKNOWN_COIN.equalsIgnoreCase(record.getCoinSymbol()))
                        .collectList()
                        .block(); // Blocking since we're no longer reactive

                logPollingSummary(records);
            } catch (Exception e) {
                log.error("Polling error: {}", e.getMessage(), e);
            }
        };

        pollingTask = executor.scheduleAtFixedRate(
                pollingRunnable,
                0,
                config.getIntervalSeconds(),
                TimeUnit.SECONDS
        );

        pollingActive.set(true);
        log.info("Polling task scheduled.");
    }

    public synchronized void stopPolling() {
        if (pollingTask != null) {
            pollingTask.cancel(true);
            pollingTask = null;
            log.info("Polling task cancelled.");
        }
        pollingActive.set(false);
    }

    public boolean isPollingActive() {
        log.info("isPollingActive(): {}", pollingActive.get());
        return pollingActive.get();
    }

    private void logPollingSummary(List<CoinAnnouncementRecord> records) {
        if (records == null || records.isEmpty()) {
            log.info("Polling complete: No new announcements were saved.");
        } else {
            log.info("Polling complete: {} new announcement(s) saved.", records.size());
        }
    }
}
