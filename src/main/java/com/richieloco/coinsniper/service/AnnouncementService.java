package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final CoinSniperConfig config;
    private final CoinAnnouncementRepository repository;
    private final WebClient webClient = WebClient.create();

    public Flux<CoinAnnouncementRecord> pollBinanceAnnouncements() {
        String url = config.getApi().getBinance().getAnnouncement().getBaseUrl();

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class) // Replace with actual JSON parsing logic
                .flatMapMany(body -> Flux.just(
                        CoinAnnouncementRecord.builder()
                                .id(UUID.randomUUID().toString())
                                .title("Example Coin Listing")
                                .coinSymbol("XYZ")
                                .announcedAt(Instant.now())
                                .delisting(false)
                                .build()
                ))
                .flatMap(repository::save);
    }
}
