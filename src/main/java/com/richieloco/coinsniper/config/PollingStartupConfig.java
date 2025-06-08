package com.richieloco.coinsniper.config;

import com.richieloco.coinsniper.service.AnnouncementPollingScheduler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PollingStartupConfig {

    @Bean
    public CommandLineRunner startPollingRunner(AnnouncementPollingScheduler scheduler) {
        return args -> scheduler.initializePolling();
    }
}
