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
        // Optional hook
    }

    @Override
    public Mono<R> assess(T context) {
        return Mono.fromCallable(() -> {
            Prompt prompt = new Prompt(List.of(new UserMessage(generatePrompt(context))));

            ChatResponse chatResponse = chatModel.call(prompt); // ✅ synchronous

            List<Generation> generations = chatResponse.getResults();
            if (generations == null || generations.isEmpty()) {
                throw new IllegalStateException("No generations returned by model");
            }

            String content = generations.getFirst().getOutput().getText();
            R result = parseAssessmentOutput(context, content.trim());
            logAssessment(context, result);
            return result;
        }).subscribeOn(Schedulers.boundedElastic()); // offload to reactive thread
    }
}
