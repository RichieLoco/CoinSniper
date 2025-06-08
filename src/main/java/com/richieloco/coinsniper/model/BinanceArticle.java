package com.richieloco.coinsniper.model;

import lombok.Data;

@Data
public class BinanceArticle {
    private long id;
    private String code;
    private String title;
    private int type;
    private long releaseDate; // in milliseconds since epoch
}