package com.richieloco.coinsniper.repo;

import com.richieloco.coinsniper.entity.on.log.TradeOrderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TradeOrderRepository extends JpaRepository<TradeOrderLog, Long> {

    // custom queries

    @Query("SELECT t FROM orders t WHERE t.timestamp > :startDate AND <= :endDate")
    List<TradeOrderLog> findOrdersMadeBetweenTwoDates(Date startDate, Date endDate);
}
