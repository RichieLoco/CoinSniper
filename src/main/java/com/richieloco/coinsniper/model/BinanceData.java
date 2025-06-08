package com.richieloco.coinsniper.model;

import lombok.Data;

import java.util.List;

@Data
public class BinanceData {
    private List<BinanceCatalog> catalogs;
}
