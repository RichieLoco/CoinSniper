package com.richieloco.coinsniper.risk.ai;

import com.richieloco.coinsniper.config.AiConfig;
import com.richieloco.coinsniper.config.AiPromptConfig;
import com.richieloco.coinsniper.entity.on.Risk;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.ai.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.ai.context.ExchangeSelectorContext;
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

    private ExchangeAssessor buildAssessor() {
        AiPromptConfig config = new AiPromptConfig();
        return new ExchangeAssessor(chatModel, repository, config);
    }

    private ExchangeSelectorContext buildContext() {
        return new ExchangeSelectorContext("ByBit, Poloniex", "WIF", "USDT, USDC");
    }

    @Test
    void testAssessRiskReturnsCorrectValue() {
        ExchangeAssessor assessor = buildAssessor();
        ExchangeSelectorContext context = buildContext();

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("0.42");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Risk result = assessor.assess(context).block();
        assertEquals(0.42, result.getRiskScore(), 0.001);
    }

    @Test
    void testAssessRiskWithNonNumericResponseShouldThrow() {
        ExchangeAssessor assessor = buildAssessor();
        ExchangeSelectorContext context = buildContext();

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("not-a-number");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NumberFormatException.class, () -> assessor.assess(context).block());
    }

    @Test
    void testAssessRiskWithEmptyResponseShouldThrow() {
        ExchangeAssessor assessor = buildAssessor();
        ExchangeSelectorContext context = buildContext();

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NumberFormatException.class, () -> assessor.assess(context).block());
    }

    @Test
    void testAssessRiskWithNullGenerationShouldThrow() {
        ExchangeAssessor assessor = buildAssessor();
        ExchangeSelectorContext context = buildContext();

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(null);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NullPointerException.class, () -> assessor.assess(context).block());
    }

    @Test
    void testAssessRiskWithNullAssistantMessageShouldThrow() {
        ExchangeAssessor assessor = buildAssessor();
        ExchangeSelectorContext context = buildContext();

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(null);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NullPointerException.class, () -> assessor.assess(context).block());
    }

    @Test
    void testAssessRiskModelThrowsException() {
        ExchangeAssessor assessor = buildAssessor();
        ExchangeSelectorContext context = buildContext();

        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("AI service error"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> assessor.assess(context).block());
        assertEquals("AI service error", exception.getMessage());
    }
}
