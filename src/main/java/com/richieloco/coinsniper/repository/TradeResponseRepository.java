package com.richieloco.coinsniper.repository;

import com.richieloco.coinsniper.entity.on.TradeResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TradeResponseRepository extends JpaRepository<TradeResponse, Long> {

    // custom queries

    @Query("SELECT t FROM trade_response t WHERE t.timestamp > :startDate AND <= :endDate")
    List<TradeResponse> findTradeResponsesMadeBetweenTwoDates(Date startDate, Date endDate);
}
