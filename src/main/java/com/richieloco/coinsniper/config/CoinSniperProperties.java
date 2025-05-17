package com.richieloco.coinsniper.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "coin-sniper")
public class CoinSniperProperties {

    private List<String> exchanges;
    private String stableCoin;
    private Api api;

    @Getter
    @Setter
    public static class Api {
        private Binance binance;
        private OnExchange onExchange;
    }

    @Getter
    @Setter
    public static class Binance {
        private Announcement announcement;

        @Getter
        @Setter
        public static class Announcement {
            private String baseUrl;
            private int type;
            private int pageNo;
            private int pageSize;
        }
    }

    @Getter
    @Setter
    public static class OnExchange {
        private TradeConfig poloniex;
        private TradeConfig bybit;

        @Getter
        @Setter
        public static class TradeConfig {
            private String baseUrl;
            private String apiKey;
            private String apiSecret;
        }
    }
}
