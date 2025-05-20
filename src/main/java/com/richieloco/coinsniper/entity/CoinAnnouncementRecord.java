package com.richieloco.coinsniper.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("coin_announcements")
public class CoinAnnouncementRecord {
    @Id
    private String id;
    private String title;
    private String coinSymbol;
    private Instant announcedAt;
    private boolean delisting;
}
