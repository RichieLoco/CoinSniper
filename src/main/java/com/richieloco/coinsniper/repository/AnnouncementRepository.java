package com.richieloco.coinsniper.repository;


import com.richieloco.coinsniper.entity.binance.AnnouncementResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository extends JpaRepository<AnnouncementResponse, Long> {
}
