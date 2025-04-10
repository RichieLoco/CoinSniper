package com.richieloco.coinsniper.service.binance;

import reactor.core.publisher.Mono;

public interface AnnouncementService<T> {

    Mono<T> fetchAnnouncements(String baseUrl, String... params);
}
