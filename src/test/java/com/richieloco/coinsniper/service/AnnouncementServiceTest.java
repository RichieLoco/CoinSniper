package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class AnnouncementServiceTest {

    @Test
    public void testPollBinanceAnnouncements() {
        CoinAnnouncementRepository repository = mock(CoinAnnouncementRepository.class);
        CoinSniperConfig config = new CoinSniperConfig();

        CoinAnnouncementRecord announcement = CoinAnnouncementRecord.builder()
                .id(UUID.randomUUID().toString())
                .coinSymbol("XYZ")
                .title("New Coin")
                .announcedAt(Instant.now())
                .delisting(false)
                .build();

        when(repository.save(any())).thenReturn(Mono.just(announcement));

        AnnouncementService service = new AnnouncementService(config, repository);

        StepVerifier.create(service.pollBinanceAnnouncements())
                .expectNextMatches(a -> a.getCoinSymbol().equals("XYZ"))
                .verifyComplete();
    }
}
