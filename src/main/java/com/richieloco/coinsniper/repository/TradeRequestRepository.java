package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.on.TradeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TradeRequestRepository extends JpaRepository<TradeRequest, Long> {

    // custom queries

    @Query("SELECT t FROM trade_request t WHERE t.timestamp > :startDate AND <= :endDate")
    List<TradeRequest> findTradeRequestsMadeBetweenTwoDates(Date startDate, Date endDate);
}
