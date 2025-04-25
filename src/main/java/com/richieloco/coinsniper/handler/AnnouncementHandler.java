package com.richieloco.coinsniper.handler;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.service.binance.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AnnouncementHandler {

    private final AnnouncementService announcementService;
    private final CoinSniperConfig.BinanceConfig config;

    public Mono<ServerResponse> getAnnouncements(ServerRequest request) {
        // Extract query parameters with fallback values (from config)
        String type = request.queryParam("type").orElse(Integer.toString(config.getType()));
        String pageNo = request.queryParam("pageNo").orElse(Integer.toString(config.getPageNo()));
        String pageSize = request.queryParam("pageSize").orElse(Integer.toString(config.getPageSize()));

        return announcementService.fetchAnnouncements(type, pageNo, pageSize)
                .flatMap(announcement -> ServerResponse.ok().bodyValue(announcement))
                .switchIfEmpty(ServerResponse.noContent().build());
    }
}
