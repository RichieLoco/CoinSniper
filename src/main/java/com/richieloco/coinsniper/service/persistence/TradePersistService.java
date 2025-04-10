package com.richieloco.coinsniper.service.persistence;

import com.richieloco.coinsniper.entity.on.TradeRequest;
import com.richieloco.coinsniper.entity.on.TradeResponse;
import com.richieloco.coinsniper.repository.TradeRequestRepository;
import com.richieloco.coinsniper.repository.TradeResponseRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class TradePersistService {

    private final TradeRequestRepository tradeReqRepo;
    private final TradeResponseRepository tradeResRepo;

    // Save

    public TradeRequest saveTradeRequest(TradeRequest tradeReq) {
        return tradeReqRepo.save(tradeReq);
    }

    public TradeResponse saveTradeResponse(TradeResponse tradeRes) {
        return tradeResRepo.save(tradeRes);
    }

    // Find all

    public List<TradeRequest> findAllTradeRequests() {
        return tradeReqRepo.findAll();
    }

    public List<TradeResponse> findAllTradeResponses() {
        return tradeResRepo.findAll();
    }

    // Custom queries

    // Find all > minDate AND < maxDate

    public List<TradeRequest> findAllTradeRequestsBetweenDates(Date startDate, Date endDate) {
        return tradeReqRepo.findTradeRequestsMadeBetweenTwoDates(startDate, endDate);
    }

    public List<TradeResponse> findAllTradeResponsesBetweenDates(Date startDate, Date endDate) {
        return tradeResRepo.findTradeResponsesMadeBetweenTwoDates(startDate, endDate);
    }
}
