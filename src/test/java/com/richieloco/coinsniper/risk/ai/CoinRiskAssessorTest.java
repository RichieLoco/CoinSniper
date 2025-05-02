package com.richieloco.coinsniper.risk.ai;

import com.richieloco.coinsniper.config.AiConfig;
import com.richieloco.coinsniper.entity.on.Risk;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.ai.CoinRiskAssessor;
import com.richieloco.coinsniper.service.risk.ai.context.CoinRiskContext;
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
class CoinRiskAssessorTest {

    @Mock private ChatModel chatModel;
    @Mock private RiskAssessmentLogRepository repository;

    private CoinRiskAssessor buildAssessor() {
        AiConfig config = new AiConfig();
        return new CoinRiskAssessor(chatModel, repository, config);
    }

    private CoinRiskContext buildContext() {
        return new CoinRiskContext("BTC", "ETH", 0.5, 0.75, 0.2);
    }

    @Test
    void testAssessRiskReturnsCorrectValue() {
        CoinRiskAssessor assessor = buildAssessor();
        CoinRiskContext context = buildContext();

        AssistantMessage assistantMessage = mock(AssistantMessage.class);
        when(assistantMessage.getText()).thenReturn("0.55");

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(assistantMessage);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        Risk result = assessor.assessRisk(context).block();
        assertEquals(0.55, result.getRiskScore(), 0.001);
    }

    @Test
    void testAssessRiskWithNonNumericResponseShouldThrow() {
        CoinRiskAssessor assessor = buildAssessor();
        CoinRiskContext context = buildContext();

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
        CoinRiskAssessor assessor = buildAssessor();
        CoinRiskContext context = buildContext();

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
        CoinRiskAssessor assessor = buildAssessor();
        CoinRiskContext context = buildContext();

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(null);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NullPointerException.class, () -> assessor.assessRisk(context).block());
    }

    @Test
    void testAssessRiskWithNullAssistantMessageShouldThrow() {
        CoinRiskAssessor assessor = buildAssessor();
        CoinRiskContext context = buildContext();

        Generation generation = mock(Generation.class);
        when(generation.getOutput()).thenReturn(null);

        ChatResponse chatResponse = mock(ChatResponse.class);
        when(chatResponse.getResult()).thenReturn(generation);

        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        assertThrows(NullPointerException.class, () -> assessor.assessRisk(context).block());
    }

    @Test
    void testAssessRiskModelThrowsException() {
        CoinRiskAssessor assessor = buildAssessor();
        CoinRiskContext context = buildContext();

        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new RuntimeException("AI failure"));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> assessor.assessRisk(context).block());
        assertEquals("AI failure", exception.getMessage());
    }
}
