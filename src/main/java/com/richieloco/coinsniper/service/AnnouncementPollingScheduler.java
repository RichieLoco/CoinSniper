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

@Slf4j
@Component
@RequiredArgsConstructor
public class AnnouncementPollingScheduler {

    private final AnnouncementCallingService service;
    private final AnnouncementPollingConfig config;
    private final CoinSniperConfig coinSniperConfig;

    private Disposable pollingSubscription;

    @PostConstruct
    public void initializePolling() {
        if (config.isEnabled()) {
            startPolling();
        }
    }

    public synchronized void startPolling() {
        if (pollingSubscription != null && !pollingSubscription.isDisposed()) return;

        var announcementCfg = coinSniperConfig.getApi().getBinance().getAnnouncement();

        pollingSubscription = Flux.interval(Duration.ofSeconds(config.getIntervalSeconds()))
                .flatMap(tick -> {
                    log.info("Polling Binance...");
                    return service.callBinanceAnnouncements(
                            announcementCfg.getType(),
                            announcementCfg.getPageNo(),
                            announcementCfg.getPageSize()
                    );
                })
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
