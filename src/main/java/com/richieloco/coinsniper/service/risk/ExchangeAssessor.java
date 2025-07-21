package com.richieloco.coinsniper.service.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.richieloco.coinsniper.config.AiPromptConfig;
import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.model.ExchangeAssessmentResponse;
import com.richieloco.coinsniper.repository.ExchangeAssessmentRepository;
import com.richieloco.coinsniper.service.risk.context.BaseAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class ExchangeAssessor extends BaseAssessor<ExchangeSelectorContext, ExchangeAssessmentRecord> {

    private final ExchangeAssessmentRepository repository;
    private final AiPromptConfig aiPromptConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeAssessor(ChatModel chatModel, ExchangeAssessmentRepository repository, AiPromptConfig aiPromptConfig) {
        super(chatModel);
        this.repository = repository;
        this.aiPromptConfig = aiPromptConfig;
    }

    @Override
    protected String generatePrompt(ExchangeSelectorContext context) {
        PromptTemplate prompt = aiPromptConfig.exchangeCoinAvailabilityPromptTemplate();

        return prompt.render(Map.of(
                "exchanges", context.exchanges(),
                "targetCoin", context.targetCoin(),
                "stableCoins", context.stableCoins()
        ));
    }

    @Override
    protected String contextType() {
        return "Exchange";
    }

    @Override
    protected ExchangeAssessmentRecord parseAssessmentOutput(ExchangeSelectorContext context, String response) {
        // Check if response contains at least one colon — minimal requirement for key:value pairs
        if (!response.contains(":")) {
            throw new RuntimeException("Failed to map LLM response: " + response);
        }

        // Convert LLM string to JSON-style format first (if needed)
        String jsonLike = response.replaceAll("(\\w[\\w ]*): ([\\w\\d .-]+)", "\"$1\": \"$2\"")
                                  .replaceAll(",\\s*", ", ");
        jsonLike = "{" + jsonLike + "}";
        try {
            ExchangeAssessmentResponse dto = objectMapper.readValue(jsonLike, ExchangeAssessmentResponse.class);

            return ExchangeAssessmentRecord.builder()
                    .contextType(contextType())
                    .contextDescription(context.toString())
                    .exchange(dto.exchange())
                    .coinListing(dto.coinListing())
                    .overallRiskScore(dto.overallRiskScore())
                    .liquidity(dto.liquidity())
                    .tradingVolume(dto.tradingVolume())
                    .tradingFees(dto.tradingFees())
                    .assessedAt(Instant.now())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map LLM response: " + response, e);
        }
    }


    @Override
    protected void logAssessment(ExchangeSelectorContext context, ExchangeAssessmentRecord record) {
        repository.save(record).subscribe(); // Reactive side effect
    }
}
