package com.richieloco.coinsniper.entity.binance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richieloco.coinsniper.util.TestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AnnouncementResponseTest {

    @Test
    void shouldDeserializeAnnouncementJson() throws Exception {
        String json = """
        {
          "code": "000000",
          "message": null,
          "messageDetail": null,
          "data": {
            "catalogs": [
              {
                "catalogId": 48,
                "parentCatalogId": null,
                "icon": "https://public.bnbstatic.com/image/cms/content/body/202202/9252ba30f961b1a20d49e622a0ecfad5.png",
                "catalogName": "New Cryptocurrency Listing",
                "description": null,
                "catalogType": 1,
                "total": 1702,
                "articles": [
                  {
                    "id": 229923,
                    "code": "917a66f6d8654c2695d858db6b7f1bff",
                    "title": "Binance Will Add StraitsX USD (XUSD) and Four (FORM) on Earn, Buy Crypto, Margin, Convert & Futures",
                    "type": 1,
                    "releaseDate": 1742369411614
                  },
                  {
                    "id": 229896,
                    "code": "49d9f5fa7c904f59bd77ae3535f699f0",
                    "title": "Notice on New Trading Pairs & Trading Bots Services on Binance Spot - 2025-03-20",
                    "type": 1,
                    "releaseDate": 1742367602547
                  }
                ],
                "catalogs": []
              }
            ]
          },
          "success": true
        }
        """;

        ObjectMapper mapper = new ObjectMapper();
        Announcement announcement = mapper.readValue(json, Announcement.class);

        assertNotNull(announcement);
        assertTrue(announcement.isSuccess());
        assertEquals("000000", announcement.getCode());
        assertNotNull(announcement.getData());
        assertEquals(1, announcement.getData().getCatalogs().size());

        Catalog catalog = announcement.getData().getCatalogs().getFirst();
        assertEquals("New Cryptocurrency Listing", catalog.getCatalogName());
        assertEquals(2, catalog.getArticles().size());
    }

    @Test
    void shouldDeserializeAnnouncementJsonFile() throws Exception {
        Announcement announcement = TestUtil.readFromFile("testResponse_Full.json", Announcement.class);
        assertNotNull(announcement);
        assertTrue(announcement.isSuccess());
        assertEquals("000000", announcement.getCode());
        assertNotNull(announcement.getData());
        assertEquals(8, announcement.getData().getCatalogs().size());

        Catalog catalog = announcement.getData().getCatalogs().getFirst();
        assertEquals("New Cryptocurrency Listing", catalog.getCatalogName());
        assertEquals(10, catalog.getArticles().size());
    }
}
