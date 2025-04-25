package com.richieloco.coinsniper.service.binance;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.binance.Announcement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AnnouncementService {
    private final WebClient webClient;
    private final CoinSniperConfig apiConfig;

    public Mono<Announcement> fetchAnnouncements(String... params) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(apiConfig.getBinance().getBaseUrl())
                        .queryParam("type", params[0])
                        .queryParam("pageNo", params[1])
                        .queryParam("pageSize", params[2])
                        .build())
                .retrieve()
                .bodyToMono(Announcement.class);
    }
}

