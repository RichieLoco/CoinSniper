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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    public void backtesting_shouldHandleEmptyData() {
        Model model = new ConcurrentModel();
        when(repository.findAll()).thenReturn(Flux.empty());

        String view = controller.backtesting(model).block();

        assertEquals("backtesting", view);
        verify(djlTrainingService).train(Collections.emptyList());
        verify(djlTrainingService).logToFile(Collections.emptyList());

        Object attr = model.getAttribute("history");
        assertEquals(Collections.emptyList(), attr);
    }

    @Test
    public void backtesting_shouldNotFailIfTrainingThrows() {
        Model model = new ConcurrentModel();
        TradeDecisionRecord record = TradeDecisionRecord.builder().coinSymbol("XYZ").build();

        when(repository.findAll()).thenReturn(Flux.just(record));
        doThrow(new RuntimeException("Training failed")).when(djlTrainingService).train(any());
        doThrow(new RuntimeException("Log failed")).when(djlTrainingService).logToFile(any());

        String view = controller.backtesting(model).block();

        assertEquals("backtesting", view); // controller should not fail
        assertEquals(List.of(record), model.getAttribute("history"));
    }

    @Test
    public void backtesting_shouldPropagateRepositoryError() {
        Model model = new ConcurrentModel();

        when(repository.findAll()).thenReturn(Flux.error(new RuntimeException("Repo failure")));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            controller.backtesting(model).block();
        });

        assertEquals("Repo failure", exception.getMessage());
    }
}
