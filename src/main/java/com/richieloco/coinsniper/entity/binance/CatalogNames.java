package com.richieloco.coinsniper.entity.binance;

import lombok.Getter;

@Getter
public enum CatalogNames {
    NEW_CRYPTO_LISTING("New Cryptocurrency Listing"),
    NEW_FIAT_LISTING("New Fiat Listings"),
    LATEST_NEWS("Latest Binance News"),
    LATEST_ACTIVITIES("Latest Activities"),
    DELISTING("Delisting"),
    MAINTENANCE_UPDATES("Maintenance Updates"),
    API_UPDATES("API Updates"),
    CRYPTO_AIRDROP("Crypto Airdrop");

    private final String name;

    CatalogNames(String name) {
        this.name = name;
    }
}
