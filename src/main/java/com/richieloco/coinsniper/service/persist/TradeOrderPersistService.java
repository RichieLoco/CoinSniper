package com.richieloco.coinsniper.service.persist;

import com.richieloco.coinsniper.entity.on.TradeOrderLog;
import com.richieloco.coinsniper.repo.TradeOrderRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class TradeOrderPersistService {

    private final TradeOrderRepository tradeOrderRepo;

    /* Included queries */

    //-- Save
    public TradeOrderLog saveTradeOrder(TradeOrderLog tradeReq) {
        return tradeOrderRepo.save(tradeReq);
    }

    //--- Find all
    public List<TradeOrderLog> findAllTradeOrders() {
        return tradeOrderRepo.findAll();
    }

    /* Custom queries */

    //--- Find all > minDate AND < maxDate
    public List<TradeOrderLog> findAllTradeRequestsBetweenDates(Date startDate, Date endDate) {
        return tradeOrderRepo.findOrdersMadeBetweenTwoDates(startDate, endDate);
    }
}
