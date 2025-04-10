package com.richieloco.coinsniper.service.persistence;

import com.richieloco.coinsniper.entity.binance.AnnouncementResponse;
import com.richieloco.coinsniper.repository.AnnouncementRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AnnouncementPersistService {

    private final AnnouncementRepository announceRepo;

    // Save

    public AnnouncementResponse saveAnnouncement(AnnouncementResponse announceRes) {
        return announceRepo.save(announceRes);
    }

    // Find all

    public List<AnnouncementResponse> findAllAnnouncements() {
        return announceRepo.findAll();
    }

}

