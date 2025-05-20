package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class DashboardControllerTest {

    @Mock
    private TradeDecisionRepository repository;

    private DashboardController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new DashboardController(repository);
    }

    @Test
    public void dashboard_shouldAddAttributeAndReturnView() {
        Model model = new ConcurrentModel();

        when(repository.findAll()).thenReturn(Flux.empty());

        String view = controller.dashboard(model);

        assertEquals("dashboard", view);
        verify(repository, times(1)).findAll();
    }
}
