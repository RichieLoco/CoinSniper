package com.richieloco.coinsniper.entity.on;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "risk_assessments")
public class RiskScore {
    private Double riskScore;
}
