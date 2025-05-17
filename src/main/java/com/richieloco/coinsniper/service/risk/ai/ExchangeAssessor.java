package com.richieloco.coinsniper.service.risk.ai;

import com.richieloco.coinsniper.config.AiPromptConfig;
import com.richieloco.coinsniper.entity.on.ExchangeAssessment;
import com.richieloco.coinsniper.entity.on.RiskLevel;
import com.richieloco.coinsniper.entity.on.log.ExchangeAssessmentLog;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.ai.context.ExchangeSelectorContext;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class ExchangeAssessor extends BaseAssessor<ExchangeSelectorContext, ExchangeAssessment> {

    private final RiskAssessmentLogRepository repository;
    private final AiPromptConfig aiPromptConfig;

    public ExchangeAssessor(ChatModel chatModel, RiskAssessmentLogRepository repository, AiPromptConfig aiPromptConfig) {
        super(chatModel);
        this.repository = repository;
        this.aiPromptConfig = aiPromptConfig;
    }

    @Override
    protected String generatePrompt(ExchangeSelectorContext context) {

        PromptTemplate prompt = aiPromptConfig.exchangeCoinRiskPromptTemplate();

        String promptString = prompt.render(Map.of(
                "fromExchange", context.fromExchange(),
                "toExchange", context.toExchange(),
                "marketVolatility", context.marketVolatility()
        ));

        FIX THE ABOVE TO MATCH THE PROMPT FIELDS IN exchangeCoinAvailabilityPromptTemplate()

        return "Assess risk for exchange(s) " + context.exchanges() + " and coin " + context.targetCoin();
    }

    @Override
    protected String contextType() {
        return "Exchange";
    }

    @Override
    protected ExchangeAssessment parseAssessmentOutput(String response) {
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

            String key = keyValue[0];
            String value = keyValue[1];

            switch (key) {
                case "Exchange":
                    exchange = value;
                    break;
                case "Coin Listing":
                    coinListing = value;
                    break;
                case "Overall Risk Score":
                    overallRiskScore = Integer.parseInt(value);
                    break;
                case "Liquidity":
                    liquidity = RiskLevel.valueOf(value);
                    break;
                case "Trading Volume":
                    tradingVolume = RiskLevel.valueOf(value);
                    break;
                case "Trading Fees":
                    tradingFees = RiskLevel.valueOf(value);
                    break;

            }
        }
        return new ExchangeAssessment(exchange, coinListing, overallRiskScore, tradingFees, tradingVolume, liquidity);
    }

    @Override
    protected void logAssessment(ExchangeSelectorContext context, ExchangeAssessment risk) {
        repository.save(new ExchangeAssessmentLog(
                null,
                contextType(),
                context.toString(),
                risk.getExchange(),
                risk.getCoinListing(),
                risk.getOverallRiskScore(),
                risk.getTradingVolume(),
                risk.getLiquidity(),
                risk.getTradingFees(),
                Instant.now()
        ));
    }
}
