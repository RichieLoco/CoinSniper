package com.richieloco.coinsniper.service.risk.ai;

import com.richieloco.coinsniper.config.AiConfig;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.ai.context.ExchangeRiskContext;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRiskAssessor extends BaseRiskAssessor<ExchangeRiskContext> {

    private final AiConfig promptConfig;

    public ExchangeRiskAssessor(ChatModel chatModel,
                                RiskAssessmentLogRepository repository,
                                AiConfig promptConfig) {
        super(chatModel, repository);
        this.promptConfig = promptConfig;
    }

    @Override
    protected String generatePrompt(ExchangeRiskContext context) {
        return String.format(
                promptConfig.exchangeRiskPromptTemplate().getTemplate(),
                context.fromExchange(),
                context.toExchange(),
                context.marketVolatility(),
                context.liquidityDifference(),
                context.feeDifference()
        );
    }

    @Override
    protected String contextType() {
        return "Exchange";
    }
}
