package com.richieloco.coinsniper.service.risk;

import com.richieloco.coinsniper.config.AiPromptConfig;
import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.entity.RiskLevel;
import com.richieloco.coinsniper.repository.ExchangeAssessmentRepository;
import com.richieloco.coinsniper.service.risk.context.BaseAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class ExchangeAssessor extends BaseAssessor<ExchangeSelectorContext, ExchangeAssessmentRecord> {

    private final ExchangeAssessmentRepository repository;
    private final AiPromptConfig aiPromptConfig;

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
        String[] parts = response.split(",\\s*");
        String exchange = null;
        String coinListing = null;
        int overallRiskScore = 0;
        RiskLevel liquidity = null;
        RiskLevel tradingVolume = null;
        RiskLevel tradingFees = null;

        for (String part : parts) {
            String[] keyValue = part.split(":\\s*", 2);
            if (keyValue.length < 2) continue;

            switch (keyValue[0]) {
                case "Exchange" -> exchange = keyValue[1];
                case "Coin Listing" -> coinListing = keyValue[1];
                case "Overall Risk Score" -> overallRiskScore = Integer.parseInt(keyValue[1]);
                case "Liquidity" -> liquidity = RiskLevel.valueOf(keyValue[1]);
                case "Trading Volume" -> tradingVolume = RiskLevel.valueOf(keyValue[1]);
                case "Trading Fees" -> tradingFees = RiskLevel.valueOf(keyValue[1]);
            }
        }

        return ExchangeAssessmentRecord.builder()
                .contextType(contextType())
                .contextDescription(context.toString())
                .exchange(exchange)
                .coinListing(coinListing)
                .overallRiskScore(overallRiskScore)
                .liquidity(String.valueOf(liquidity))
                .tradingVolume(String.valueOf(tradingVolume))
                .tradingFees(String.valueOf(tradingFees))
                .assessedAt(Instant.now())
                .build();
    }

    @Override
    protected void logAssessment(ExchangeSelectorContext context, ExchangeAssessmentRecord record) {
        repository.save(record).subscribe(); // Reactive side-effect
    }
}
