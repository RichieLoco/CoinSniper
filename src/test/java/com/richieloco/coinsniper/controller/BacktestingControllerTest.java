package com.richieloco.coinsniper.controller;

import com.richieloco.coinsniper.entity.TradeDecisionRecord;
import com.richieloco.coinsniper.repository.TradeDecisionRepository;
import com.richieloco.coinsniper.service.DJLTrainingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import reactor.core.publisher.Flux;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BacktestingControllerTest {

    @Mock
    private TradeDecisionRepository repository;

    @Mock
    private DJLTrainingService djlTrainingService;

    private BacktestingController controller;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new BacktestingController(repository, djlTrainingService);
    }

    @Test
    public void backtesting_shouldAddHistoryAndReturnView() {
        Model model = new ConcurrentModel();

        TradeDecisionRecord record = TradeDecisionRecord.builder().coinSymbol("XYZ").build();
        when(repository.findAll()).thenReturn(Flux.just(record));

        String view = controller.backtesting(model).block(); // ✅ block to get the result

        assertEquals("backtesting", view);
        verify(djlTrainingService).train(Collections.singletonList(record));
        verify(djlTrainingService).logToFile(Collections.singletonList(record));
    }

}
