package com.richieloco.coinsniper.entity.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnnouncementResponse {
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
