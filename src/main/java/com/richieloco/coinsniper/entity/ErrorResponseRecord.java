package com.richieloco.coinsniper.entity;

import jakarta.persistence.GeneratedValue;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("error_responses")
public class ErrorResponseRecord implements Identifiable {
    @Id
    @GeneratedValue
    private UUID id;
    private String source;
    private String errorMessage;
    private int statusCode;
    private Instant timestamp;
}
