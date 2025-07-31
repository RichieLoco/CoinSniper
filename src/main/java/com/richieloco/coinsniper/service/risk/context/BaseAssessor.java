package com.richieloco.coinsniper.service.risk.context;

import com.richieloco.coinsniper.service.risk.AssessmentFunction;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
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
    protected abstract R parseAssessmentOutput(T context, String response);
    protected void logAssessment(T context, R assessment) {
        // Optional hook for persisting or logging
    }

    protected Mono<String> generateAssessment(T context) {
        return Mono.fromCallable(() -> {
                    String prompt = generatePrompt(context);
                    ChatResponse chatResponse = chatModel.call(new Prompt(prompt)); // still blocking

                    if (chatResponse == null
                            || chatResponse.getResults().isEmpty()
                            || chatResponse.getResults().getFirst().getOutput() == null
                            || chatResponse.getResults().getFirst().getOutput().getText() == null
                            || chatResponse.getResults().getFirst().getOutput().getText().trim().isEmpty()) {
                        throw new NullPointerException("LLM generation contained no usable output");
                    }

                    return chatResponse.getResults().getFirst().getOutput().getText();
                })
                .subscribeOn(Schedulers.boundedElastic()); // move off Netty event loop
    }

    @Override
    public Mono<R> assess(T context) {
        return generateAssessment(context)
                .map(response -> {
                    R result = parseAssessmentOutput(context, response);
                    logAssessment(context, result);
                    return result;
                });
    }
}
