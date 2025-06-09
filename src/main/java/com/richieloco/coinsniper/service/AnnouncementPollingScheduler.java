package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.AnnouncementPollingConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static com.richieloco.coinsniper.service.AnnouncementCallingService.UNKNOWN_COIN;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementPollingScheduler {

    protected final AnnouncementCallingService service;
    protected final AnnouncementPollingConfig config;
    protected final CoinSniperConfig coinSniperConfig;

    private Disposable pollingSubscription;

    public void initializePolling() {
        if (config.isEnabled()) {
            startPolling();
        }
    }

    public synchronized void startPolling() {
        if (!config.isEnabled()) return;

        if (pollingSubscription != null && !pollingSubscription.isDisposed()) return;

        var announcementCfg = coinSniperConfig.getApi().getBinance().getAnnouncement();

        pollingSubscription = Flux.interval(Duration.ofSeconds(config.getIntervalSeconds()))
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
                .onErrorContinue((ex, obj) -> log.error("Polling error: {}", ex.getMessage()))
                .subscribe(record -> log.info("Saved: {}", record.getCoinSymbol()));
    }

    public synchronized void stopPolling() {
        if (pollingSubscription != null) {
            pollingSubscription.dispose();
            pollingSubscription = null;
        }
    }

    public synchronized boolean isPollingActive() {
        return pollingSubscription != null && !pollingSubscription.isDisposed();
    }
}
