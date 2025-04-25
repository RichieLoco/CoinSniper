package com.richieloco.coinsniper.service.persist;

import com.richieloco.coinsniper.entity.binance.Announcement;
import com.richieloco.coinsniper.repo.AnnouncementRepository;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AnnouncementPersistService {

    private final AnnouncementRepository announceRepo;

    /* Included queries */

    //-- Save
    public Announcement saveAnnouncement(Announcement announceRes) {
        return announceRepo.save(announceRes);
    }

    // Find all
    public List<Announcement> findAllAnnouncements() {
        return announceRepo.findAll();
    }

}

