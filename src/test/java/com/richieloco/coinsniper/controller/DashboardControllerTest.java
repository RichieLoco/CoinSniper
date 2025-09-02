package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.DashboardConfig;
import com.richieloco.coinsniper.entity.CoinAnnouncementRecord;
import com.richieloco.coinsniper.entity.ErrorResponseRecord;
import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.CoinAnnouncementRepository;
import com.richieloco.coinsniper.repository.ErrorResponseRepository;
import com.richieloco.coinsniper.repository.ExchangeAssessmentRepository;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DashboardControllerTest {

    @Mock private TradeDecisionRepository tradeRepo;
    @Mock private CoinAnnouncementRepository announcementRepo;
    @Mock private ExchangeAssessmentRepository assessmentRepo;
    @Mock private ErrorResponseRepository errorRepo;
    @Mock private DashboardConfig dashboardConfig;

    private DashboardController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new DashboardController(tradeRepo, announcementRepo, assessmentRepo, errorRepo, dashboardConfig);
    }

    @Test
    public void viewDashboard_shouldAddAllAttributes_whenEmpty() {
        Model model = new ConcurrentModel();

        when(tradeRepo.findAll()).thenReturn(Flux.empty());
        when(announcementRepo.findAll()).thenReturn(Flux.empty());
        when(assessmentRepo.findAll()).thenReturn(Flux.empty());
        when(errorRepo.findAll()).thenReturn(Flux.empty());

        String view = controller.viewDashboard(model).block();

        assertEquals("dashboard", view);
        assertEquals(dashboardConfig, model.getAttribute("dashboard"));

        assertTrue(((List<?>) model.getAttribute("trades")).isEmpty());
        assertTrue(((List<?>) model.getAttribute("announcements")).isEmpty());
        assertTrue(((List<?>) model.getAttribute("assessments")).isEmpty());
        assertTrue(((List<?>) model.getAttribute("errors")).isEmpty());
    }

    @Test
    public void viewDashboard_shouldAddAllAttributes_whenDataPresent() {
        Model model = new ConcurrentModel();
        Instant now = Instant.now();

        TradeDecisionRecord trade = TradeDecisionRecord.builder()
                .coinSymbol("BTC").exchange("Binance").riskScore(4.5).tradeExecuted(true).decidedAt(now).build();

        CoinAnnouncementRecord ann = CoinAnnouncementRecord.builder()
                .coinSymbol("BTC").title("BTC Listing").announcedAt(now).build();

        ExchangeAssessmentRecord assess = ExchangeAssessmentRecord.builder()
                .coinListing("BTCUSDT").exchange("Binance").overallRiskScore("MEDIUM").assessedAt(now).build();

        ErrorResponseRecord error = ErrorResponseRecord.builder()
                .id(UUID.randomUUID()).source("Binance").errorMessage("Timeout").statusCode(504).timestamp(now).build();

        when(tradeRepo.findAll()).thenReturn(Flux.just(trade));
        when(announcementRepo.findAll()).thenReturn(Flux.just(ann));
        when(assessmentRepo.findAll()).thenReturn(Flux.just(assess));
        when(errorRepo.findAll()).thenReturn(Flux.just(error));

        String view = controller.viewDashboard(model).block();

        assertEquals("dashboard", view);
        assertEquals(dashboardConfig, model.getAttribute("dashboard"));

        assertEquals(1, ((List<?>) model.getAttribute("trades")).size());
        assertEquals(1, ((List<?>) model.getAttribute("announcements")).size());
        assertEquals(1, ((List<?>) model.getAttribute("assessments")).size());
        assertEquals(1, ((List<?>) model.getAttribute("errors")).size());
    }

    @Test
    public void viewDashboard_shouldHandleRepositoryError() {
        Model model = new ConcurrentModel();

        when(tradeRepo.findAll()).thenReturn(Flux.error(new RuntimeException("Database error")));
        when(announcementRepo.findAll()).thenReturn(Flux.empty());
        when(assessmentRepo.findAll()).thenReturn(Flux.empty());
        when(errorRepo.findAll()).thenReturn(Flux.empty());

        assertThrows(RuntimeException.class, () -> controller.viewDashboard(model).block());
    }

    @Test
    public void viewDashboard_shouldPreserveExistingModelAttributes() {
        Model model = new ConcurrentModel();
        model.addAttribute("foo", "bar");

        when(tradeRepo.findAll()).thenReturn(Flux.empty());
        when(announcementRepo.findAll()).thenReturn(Flux.empty());
        when(assessmentRepo.findAll()).thenReturn(Flux.empty());
        when(errorRepo.findAll()).thenReturn(Flux.empty());

        String view = controller.viewDashboard(model).block();

        assertEquals("dashboard", view);
        assertEquals("bar", model.getAttribute("foo"));
    }

    @Test
    public void viewDashboard_shouldWorkWithoutDashboardConfig() {
        controller = new DashboardController(tradeRepo, announcementRepo, assessmentRepo, errorRepo, null);
        Model model = new ConcurrentModel();

        when(tradeRepo.findAll()).thenReturn(Flux.empty());
        when(announcementRepo.findAll()).thenReturn(Flux.empty());
        when(assessmentRepo.findAll()).thenReturn(Flux.empty());
        when(errorRepo.findAll()).thenReturn(Flux.empty());

        String view = controller.viewDashboard(model).block();

        assertEquals("dashboard", view);
        assertNull(model.getAttribute("dashboard"));
    }
}
