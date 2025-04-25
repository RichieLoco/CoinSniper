package com.richieloco.coinsniper.service.risk.ai;

import com.richieloco.coinsniper.config.AiConfig;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.ai.context.CoinRiskContext;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class CoinRiskAssessor extends BaseRiskAssessor<CoinRiskContext> {

    private final AiConfig promptConfig;

    public CoinRiskAssessor(ChatModel chatModel,
                            RiskAssessmentLogRepository repository,
                            AiConfig promptConfig) {
        super(chatModel, repository);
        this.promptConfig = promptConfig;
    }

    @Override
    protected String generatePrompt(CoinRiskContext context) {
        return String.format(
                promptConfig.coinRiskPromptTemplate().getTemplate(),
                context.coinA(),
                context.coinB(),
                context.historicalVolatility(),
                context.correlation(),
                context.volumeDifference()
        );
    }

    @Override
    protected String contextType() {
        return "Coin";
    }
}
