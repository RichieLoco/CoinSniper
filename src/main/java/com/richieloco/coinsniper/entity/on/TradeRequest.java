package com.richieloco.coinsniper.entity.on;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "trade_request")
public class TradeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotNull(message = "Order side is required")
    private OrderSide side;

    @NotNull(message = "Order type is required")
    private OrderType type;

    @NotNull(message = "Timestamp is required")
    private Long timestamp;

    private TimeInForce timeInForce;
    private BigDecimal quantity;
    private BigDecimal quoteOrderQty;
    private BigDecimal price;
    private String newClientOrderId;

    private Long strategyId;

    @Min(value = 1000000, message = "Strategy type cannot be less than 1000000")
    private Integer strategyType;

    private BigDecimal stopPrice;
    private Long trailingDelta;
    private BigDecimal icebergQty;
    private OrderResponseType newOrderRespType;
    private SelfTradePreventionMode selfTradePreventionMode;

    @Max(value = 60000, message = "recvWindow cannot be greater than 60000")
    private Long recvWindow;

    public enum OrderSide {
        BUY, SELL
    }

    enum OrderType {
        LIMIT, MARKET, STOP_LOSS, STOP_LOSS_LIMIT, TAKE_PROFIT, TAKE_PROFIT_LIMIT, TRAILING_STOP_MARKET
    }

    enum TimeInForce {
        GTC, IOC, FOK, GTX
    }

    enum OrderResponseType {
        ACK, RESULT, FULL
    }

    enum SelfTradePreventionMode {
        EXPIRE_TAKER, EXPIRE_MAKER, EXPIRE_BOTH, NONE
    }
}