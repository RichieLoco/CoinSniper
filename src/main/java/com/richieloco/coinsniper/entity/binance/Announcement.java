package com.richieloco.coinsniper.entity.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

@lombok.Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "announcements")
public class Announcement {
    private String code;
    private String message;
    private String messageDetail;
    private Data data;
    private boolean success;
}


@lombok.Data
class Data {
    private List<Catalog> catalogs;
}


@lombok.Data
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

@lombok.Data
class Article {
    private int id;
    private String code;
    private String title;
    private int type;
    private long releaseDate;
}
