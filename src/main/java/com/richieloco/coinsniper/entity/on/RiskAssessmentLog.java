package com.richieloco.coinsniper.entity.on;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Entity
@Table(name = "risk")
public class RiskAssessmentLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contextType;
    private String contextDescription;
    private double riskScore;
    private Instant assessedAt;
}
