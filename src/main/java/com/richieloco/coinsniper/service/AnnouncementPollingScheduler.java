package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.AnnouncementPollingConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.richieloco.coinsniper.service.AnnouncementCallingService.UNKNOWN_COIN;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementPollingScheduler {

    protected final AnnouncementCallingService service;
    protected final AnnouncementPollingConfig config;
    protected final CoinSniperConfig coinSniperConfig;

    private final AtomicReference<Disposable> pollingSubscriptionRef = new AtomicReference<>();

    public void initializePolling() {
        if (config.isEnabled()) {
            startPolling();
        }
    }

    public void startPolling() {
        if (!config.isEnabled()) return;

        Disposable current = pollingSubscriptionRef.get();
        if (current != null && !current.isDisposed()) return;

        var announcementCfg = coinSniperConfig.getApi().getBinance().getAnnouncement();

        Disposable subscription = Flux.interval(Duration.ofSeconds(config.getIntervalSeconds()))
                .startWith(0L)
                .flatMap(tick -> {
                    log.info("Polling Binance...");
                    return service.callBinanceAnnouncements(
                            announcementCfg.getType(),
                            announcementCfg.getPageNo(),
                            announcementCfg.getPageSize()
                    );
                })
                .filter(record -> !UNKNOWN_COIN.equalsIgnoreCase(record.getCoinSymbol()))
                .collectList()
                .doOnNext(this::logPollingSummary)
                .repeat()
                .onErrorContinue((ex, obj) -> log.error("Polling error: {}", ex.getMessage()))
                .subscribe();

        pollingSubscriptionRef.set(subscription);
    }

    private void logPollingSummary(List<CoinAnnouncementRecord> records) {
        if (records.isEmpty()) {
            log.info("Polling complete: No new announcements were saved.");
        } else {
            log.info("Polling complete: {} new announcement(s) saved.", records.size());
        }
    }

    public void stopPolling() {
        Disposable current = pollingSubscriptionRef.getAndSet(null);
        if (current != null) {
            current.dispose();
        }
    }

    public boolean isPollingActive() {
        Disposable current = pollingSubscriptionRef.get();
        return current != null && !current.isDisposed();
    }
}
