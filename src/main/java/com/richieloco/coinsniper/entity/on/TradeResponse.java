package com.richieloco.coinsniper.entity.on;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "trade_response")
public class TradeResponse {

        private String clientOrderId;
        private BigDecimal cumQty;
        private BigDecimal cumQuote;
        private BigDecimal executedQty;
        private long orderId;
        private BigDecimal avgPrice;
        private BigDecimal origQty;
        private BigDecimal price;
        private boolean reduceOnly;
        private String side;
        private String positionSide;
        private String status;
        private BigDecimal stopPrice;
        private boolean closePosition;
        private String symbol;
        private String timeInForce;
        private String type;
        private String origType;
        private BigDecimal activatePrice;
        private BigDecimal priceRate;
        private Instant updateTime;
        private String workingType;
        private boolean priceProtect;
        private String priceMatch;
        private String selfTradePreventionMode;
        private Instant goodTillDate;

}
