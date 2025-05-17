package com.richieloco.coinsniper.service.risk.ai;

import com.richieloco.coinsniper.service.risk.AssessmentFunction;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

public abstract class BaseAssessor<T, R> implements AssessmentFunction<T, R> {

    protected final ChatModel chatModel;

    protected BaseAssessor(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    protected abstract String generatePrompt(T context);

    protected abstract String contextType();

    protected abstract R parseAssessmentOutput(String response);

    protected void logAssessment(T context, R assessment) {
        // No-op by default. Subclasses may override to store to DB.
    }

    @Override
    public Mono<R> assess(T context) {
        return Mono.fromCallable(() -> {
            Prompt prompt = new Prompt(List.of(new UserMessage(generatePrompt(context))));

            String response = chatModel.call(prompt)
                    .getResult()
                    .getOutput()
                    .getText()
                    .trim();

            R assessment = parseAssessmentOutput(response, context);

            logAssessment(context, assessment);

            return assessment;
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
