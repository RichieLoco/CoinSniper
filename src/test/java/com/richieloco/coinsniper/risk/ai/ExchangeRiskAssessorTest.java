package com.richieloco.coinsniper.risk.ai;

import com.richieloco.coinsniper.config.AiConfig;
import com.richieloco.coinsniper.entity.on.Risk;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ExchangeRiskAssessorTest {

    @Mock private ChatModel chatModel;
    @Mock private RiskAssessmentLogRepository repository;

    private ExchangeRiskAssessor buildAssessor() {
        AiConfig config = new AiConfig();
        return new ExchangeRiskAssessor(chatModel, repository, config);
    }

    private ExchangeRiskContext buildContext() {
        return new ExchangeRiskContext("Binance", "Kraken", 0.7, 0.3, 0.001);
    }

    @Test
    void testAssessRiskReturnsCorrectValue() {
        ExchangeRiskAssessor assessor = buildAssessor();
        ExchangeRiskContext context = buildContext();

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("0.42");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Risk result = assessor.assessRisk(context).block();
        assertEquals(0.42, result.getRiskScore(), 0.001);
    }

    @Test
    void testAssessRiskWithNonNumericResponseShouldThrow() {
        ExchangeRiskAssessor assessor = buildAssessor();
        ExchangeRiskContext context = buildContext();

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("not-a-number");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NumberFormatException.class, () -> assessor.assessRisk(context).block());
    }

    @Test
    void testAssessRiskWithEmptyResponseShouldThrow() {
        ExchangeRiskAssessor assessor = buildAssessor();
        ExchangeRiskContext context = buildContext();

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NumberFormatException.class, () -> assessor.assessRisk(context).block());
    }

    @Test
    void testAssessRiskWithNullGenerationShouldThrow() {
        ExchangeRiskAssessor assessor = buildAssessor();
        ExchangeRiskContext context = buildContext();

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(null);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NullPointerException.class, () -> assessor.assessRisk(context).block());
    }

    @Test
    void testAssessRiskWithNullAssistantMessageShouldThrow() {
        ExchangeRiskAssessor assessor = buildAssessor();
        ExchangeRiskContext context = buildContext();

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(null);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NullPointerException.class, () -> assessor.assessRisk(context).block());
    }

    @Test
    void testAssessRiskModelThrowsException() {
        ExchangeRiskAssessor assessor = buildAssessor();
        ExchangeRiskContext context = buildContext();

        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("AI service error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> assessor.assessRisk(context).block());
        assertEquals("AI service error", exception.getMessage());
    }
}
