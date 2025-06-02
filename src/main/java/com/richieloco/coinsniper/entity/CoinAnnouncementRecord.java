package com.richieloco.coinsniper.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("coin_announcements")
public class CoinAnnouncementRecord implements Identifiable {
    @Id
    private UUID id;
    private String title;
    private String coinSymbol;
    private Instant announcedAt;
    private boolean delisting;
}
