package com.richieloco.coinsniper.risk.ai;

import com.richieloco.coinsniper.config.AiConfig;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.ai.ExchangeRiskAssessor;
import com.richieloco.coinsniper.service.risk.ai.context.ExchangeRiskContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ExchangeRiskAssessorTest {

    @Mock private ChatModel chatModel;
    @Mock private RiskAssessmentLogRepository repository;

    @Test
    void testAssessRiskReturnsCorrectValue() {
        // Arrange
        AiConfig config = new AiConfig();
        ExchangeRiskAssessor assessor = new ExchangeRiskAssessor(chatModel, repository, config);
        ExchangeRiskContext context = new ExchangeRiskContext("Binance", "Kraken", 0.7, 0.3, 0.001);

        // Mocking the response
        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("0.42");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        // Act
        double result = assessor.assessRisk(context);

        // Assert
        assertEquals(0.42, result, 0.001);
    }
}