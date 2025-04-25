package com.richieloco.coinsniper.service.risk.ai;

import com.richieloco.coinsniper.entity.on.RiskAssessmentLog;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.RiskAssessmentFunction;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;

import java.time.Instant;
import java.util.List;

public abstract class BaseRiskAssessor<T> implements RiskAssessmentFunction<T> {

    protected final ChatModel chatModel;
    protected final RiskAssessmentLogRepository repository;

    protected BaseRiskAssessor(ChatModel chatModel, RiskAssessmentLogRepository repository) {
        this.chatModel = chatModel;
        this.repository = repository;
    }

    protected abstract String generatePrompt(T context);
    protected abstract String contextType();

    @Override
    public double assessRisk(T context) {
        Prompt prompt = new Prompt(List.of(new UserMessage(generatePrompt(context))));

        String response = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText()
                //.getContent()
                .trim();

        double risk = Double.parseDouble(response);

        repository.save(new RiskAssessmentLog(
                null,
                contextType(),
                context.toString(),
                risk,
                Instant.now()
        ));
        return risk;
    }
}
