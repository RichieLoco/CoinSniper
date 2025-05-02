package com.richieloco.coinsniper.service.risk.ai;

import com.richieloco.coinsniper.entity.on.Risk;
import com.richieloco.coinsniper.entity.on.log.RiskAssessmentLog;
import com.richieloco.coinsniper.repo.RiskAssessmentLogRepository;
import com.richieloco.coinsniper.service.risk.RiskAssessmentFunction;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    public Mono<Risk> assessRisk(T context) {
        return Mono.fromCallable(() -> {
            Prompt prompt = new Prompt(List.of(new UserMessage(generatePrompt(context))));

            String response = chatModel.call(prompt)
                    .getResult()
                    .getOutput()
                    .getText()
                    .trim();

            Risk risk = new Risk(Double.parseDouble(response));

            repository.save(new RiskAssessmentLog(
                    null,
                    contextType(),
                    context.toString(),
                    risk.getRiskScore(),
                    Instant.now()
            ));

            return risk;
        }).subscribeOn(Schedulers.boundedElastic()); // non-blocking thread pool for IO
    }
}
