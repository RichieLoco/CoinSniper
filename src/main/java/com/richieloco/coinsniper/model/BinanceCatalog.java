package com.richieloco.coinsniper.model;

import lombok.Data;
import java.util.List;

@Data
public class BinanceCatalog {
    private String catalogName;
    private List<BinanceArticle> articles;
}