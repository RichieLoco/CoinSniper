package com.richieloco.coinsniper.model;

import java.util.List;

@lombok.Data
public class BinanceResponse {
    private String code;
    private String message;
    private String messageDetail;
    private Data data;
    private boolean success;
}

class Data {
    private List<Catalog> catalogs;
}

class Catalog {
    private int catalogId;
    private Integer parentCatalogId;
    private String icon;
    private String catalogName;
    private String description;
    private int catalogType;
    private int total;
    private List<Article> articles;
    private List<Catalog> catalogs;
}

class Article {
    private int id;
    private String code;
    private String title;
    private int type;
    private long releaseDate;
}
