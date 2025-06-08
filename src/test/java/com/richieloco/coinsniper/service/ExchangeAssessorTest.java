package com.richieloco.coinsniper.service;

import com.richieloco.coinsniper.config.AiPromptConfig;
import com.richieloco.coinsniper.entity.RiskLevel;
import com.richieloco.coinsniper.repository.ExchangeAssessmentRepository;
import com.richieloco.coinsniper.service.risk.ExchangeAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ExchangeAssessorTest {

    @Mock private ChatModel chatModel;
    @Mock private ExchangeAssessmentRepository repository;
    @Mock private AiPromptConfig promptConfig;
    @Mock private PromptTemplate promptTemplate;

    private ExchangeAssessor assessor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(promptConfig.exchangeCoinAvailabilityPromptTemplate()).thenReturn(promptTemplate);
        when(promptTemplate.render(any(Map.class))).thenReturn("Rendered Prompt");

        assessor = new ExchangeAssessor(chatModel, repository, promptConfig);
    }

    @Test
    public void testAssess_returnsParsedAssessment() {
        String aiResponse = "Exchange: Binance, Coin Listing: XYZUSDT, Overall Risk Score: 3, Liquidity: Low, Trading Volume: Medium, Trading Fees: Low";
        ExchangeSelectorContext context = new ExchangeSelectorContext("Binance", "XYZ", "USDT");

        AssistantMessage message = new AssistantMessage(aiResponse);
        Generation generation = new Generation(message);
        ChatResponse response = new ChatResponse(List.of(generation));

        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(assessor.assess(context))
                .expectNextMatches(result ->
                        result.getExchange().equals("Binance") &&
                                result.getCoinListing().equals("XYZUSDT") &&
                                result.getOverallRiskScore() == 3 &&
                                Objects.equals(result.getLiquidity(), String.valueOf(RiskLevel.Low)) &&
                                Objects.equals(result.getTradingVolume(), String.valueOf(RiskLevel.Medium)) &&
                                Objects.equals(result.getTradingFees(), String.valueOf(RiskLevel.Low))
                )
                .verifyComplete();
    }

    @Test
    public void testAssess_handlesMalformedResponse() {
        String malformedResponse = "Something unexpected with no structure";
        ExchangeSelectorContext context = new ExchangeSelectorContext("Binance", "XYZ", "USDT");

        AssistantMessage message = new AssistantMessage(malformedResponse);
        Generation generation = new Generation(message);
        ChatResponse response = new ChatResponse(List.of(generation));

        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(repository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(assessor.assess(context))
                .expectNextMatches(result -> result.getExchange() == null || result.getCoinListing() == null)
                .verifyComplete(); // Should not throw, just parse with missing fields
    }

    @Test
    public void testAssess_handlesNullGeneration() {
        AssistantMessage nullMessage = new AssistantMessage((String) null);
        Generation generation = new Generation(nullMessage);
        ChatResponse response = new ChatResponse(List.of(generation));

        ExchangeSelectorContext context = new ExchangeSelectorContext("Binance", "XYZ", "USDT");

        when(chatModel.call(any(Prompt.class))).thenReturn(response);

        StepVerifier.create(assessor.assess(context))
                .expectErrorMatches(error -> error instanceof NullPointerException || error.getMessage().contains("null"))
                .verify();
    }

    @Test
    public void testGeneratePrompt_withMissingPromptTemplate() {
        ExchangeSelectorContext context = new ExchangeSelectorContext("Binance", "XYZ", "USDT");
        when(promptConfig.exchangeCoinAvailabilityPromptTemplate()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> {
            assessor.assess(context).block(); // Will fail trying to call render() on null
        });
    }

    @Test
    public void testAssess_handlesRepositoryErrorGracefully() {
        String aiResponse = "Exchange: Binance, Coin Listing: XYZUSDT, Overall Risk Score: 3, Liquidity: Low, Trading Volume: Medium, Trading Fees: Low";
        ExchangeSelectorContext context = new ExchangeSelectorContext("Binance", "XYZ", "USDT");

        AssistantMessage message = new AssistantMessage(aiResponse);
        Generation generation = new Generation(message);
        ChatResponse response = new ChatResponse(List.of(generation));

        when(chatModel.call(any(Prompt.class))).thenReturn(response);
        when(repository.save(any())).thenReturn(Mono.error(new RuntimeException("DB Save Failed")));

        StepVerifier.create(assessor.assess(context))
                .expectNextMatches(result -> result.getExchange().equals("Binance"))
                .verifyComplete(); // Should still return parsed object even if save fails
    }
}
