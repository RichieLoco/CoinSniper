package com.richieloco.coinsniper.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "coin-sniper")
@Data
public class CoinSniperConfig {
    private Supported supported;
    private Api api;

    @Data
    public static class Supported {
        private List<String> exchanges;
        private List<String> stableCoins;
    }

    @Data
    public static class Api {
        private Binance binance;
        private Map<String, OnExchange> onExchange;

        @Data
        public static class Binance {
            private Announcement announcement;

            @Data
            public static class Announcement {
                private String baseUrl;
                private int type;
                private int pageNo;
                private int pageSize;
            }
        }

        @Data
        public static class OnExchange {
            private Trade trade;

            @Data
            public static class Trade {
                private String baseUrl;
                private String apiKey;
                private String apiSecret;
            }
        }
    }
}
