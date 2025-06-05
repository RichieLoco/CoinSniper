package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.config.DashboardConfig;
import com.richieloco.coinsniper.entity.TradeDecisionRecord;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DashboardControllerTest {

    @Mock
    private TradeDecisionRepository repository;

    @Mock
    private DashboardConfig dashboardConfig;

    private DashboardController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new DashboardController(repository, dashboardConfig);
    }

    @Test
    public void viewDashboard_shouldAddAttributesAndReturnView_whenEmpty() {
        Model model = new ConcurrentModel();

        when(repository.findAll()).thenReturn(Flux.empty());

        String view = controller.viewDashboard(model).block();

        assertEquals("dashboard", view);
        verify(repository, times(1)).findAll();

        Object tradesAttr = model.getAttribute("trades");
        assertNotNull(tradesAttr);
        assertTrue(((List<?>) tradesAttr).isEmpty(), "Expected trades list to be empty");

        assertEquals(dashboardConfig, model.getAttribute("dashboard"));
    }

    @Test
    public void viewDashboard_shouldAddAttributesAndReturnView_whenDataPresent() {
        Model model = new ConcurrentModel();

        TradeDecisionRecord record = TradeDecisionRecord.builder()
                .coinSymbol("BTC")
                .exchange("Binance")
                .riskScore(4.5)
                .tradeExecuted(true)
                .timestamp(Instant.now())
                .build();

        when(repository.findAll()).thenReturn(Flux.just(record));

        String view = controller.viewDashboard(model).block();

        assertEquals("dashboard", view);
        verify(repository, times(1)).findAll();

        Object tradesAttr = model.getAttribute("trades");
        assertNotNull(tradesAttr);
        List<?> trades = (List<?>) tradesAttr;
        assertEquals(1, trades.size());

        TradeDecisionRecord result = (TradeDecisionRecord) trades.get(0);
        assertEquals("BTC", result.getCoinSymbol());
        assertTrue(result.isTradeExecuted());

        assertEquals(dashboardConfig, model.getAttribute("dashboard"));
    }
}
