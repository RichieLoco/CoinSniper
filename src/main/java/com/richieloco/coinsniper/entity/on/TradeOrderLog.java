package com.richieloco.coinsniper.entity.on;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class TradeOrderLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Order quantity is required")
    private Double quantity;

    @NotNull(message = "Order price is required")
    private Double price;
    
    @NotBlank(message = "Order symbol is required")
    private String symbol;

    @NotNull(message = "Order side is required")
    private OrderSide side;

    @NotNull(message = "Order type is required")
    private OrderType type;

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    @NotNull(message = "Order timestamp is required")
    private Instant timestamp;

    @NotNull(message = "Order strategy ID is required")
    private Long strategyId;

    public enum OrderSide {
        BUY, SELL
    }

    enum OrderType {
        LIMIT, MARKET, STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, TAKE_PROFIT_LIMIT, TRAILING_STOP_MARKET
    }

    enum OrderStatus {
        OPEN, CLOSED, CANCELLED
    }
}