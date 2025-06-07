package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.AnnouncementPollingConfig;
import com.richieloco.coinsniper.config.CoinSniperConfig;
import com.richieloco.coinsniper.config.NoSecurityTestConfig;
import com.richieloco.coinsniper.service.AnnouncementCallingService;
import com.richieloco.coinsniper.service.AnnouncementPollingScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@WebFluxTest(controllers = AnnouncementPollController.class)
@Import({NoSecurityTestConfig.class, AnnouncementPollControllerTest.SchedulerTestConfig.class})
class AnnouncementPollControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private TestableScheduler scheduler;

    @Test
    void testStartPollingEndpoint() {
        webTestClient.post()
                .uri("/api/announcements/poll/start")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("Polling started."));

        assertThat(scheduler.startCalled.get()).isTrue();
    }

    @Test
    void testStopPollingEndpoint() {
        webTestClient.post()
                .uri("/api/announcements/poll/stop")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("Polling stopped."));

        assertThat(scheduler.stopCalled.get()).isTrue();
    }

    @Test
    void testPollingStatusActive() {
        scheduler.setPollingActive(true);

        webTestClient.get()
                .uri("/api/announcements/poll/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("Polling is active."));
    }

    @Test
    void testPollingStatusStopped() {
        scheduler.setPollingActive(false);

        webTestClient.get()
                .uri("/api/announcements/poll/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).isEqualTo("Polling is stopped."));
    }

    @TestConfiguration
    static class SchedulerTestConfig {
        @Bean
        public TestableScheduler scheduler() {
            AnnouncementCallingService mockService = mock(AnnouncementCallingService.class);
            AnnouncementPollingConfig dummyConfig = new AnnouncementPollingConfig();
            CoinSniperConfig dummyCoinSniperConfig = mock(CoinSniperConfig.class); // ✅ Add this
            return new TestableScheduler(mockService, dummyConfig, dummyCoinSniperConfig);
        }
    }

    static class TestableScheduler extends AnnouncementPollingScheduler {
        AtomicBoolean pollingActive = new AtomicBoolean(false);
        AtomicBoolean startCalled = new AtomicBoolean(false);
        AtomicBoolean stopCalled = new AtomicBoolean(false);

        public TestableScheduler(AnnouncementCallingService service,
                                 AnnouncementPollingConfig config,
                                 CoinSniperConfig coinSniperConfig) {
            super(service, config, coinSniperConfig);
        }


        @Override
        public void startPolling() {
            startCalled.set(true);
            pollingActive.set(true);
        }

        @Override
        public void stopPolling() {
            stopCalled.set(true);
            pollingActive.set(false);
        }

        @Override
        public boolean isPollingActive() {
            return pollingActive.get();
        }

        public void setPollingActive(boolean active) {
            pollingActive.set(active);
        }
    }
}
