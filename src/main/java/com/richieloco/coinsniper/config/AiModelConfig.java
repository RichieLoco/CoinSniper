package com.richieloco.coinsniper.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiModelConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Bean
    public OpenAiApi openAiApi() {
        return new OpenAiApi.Builder().apiKey(apiKey).build();
    }

    @Bean
    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
                .model("gpt-4-turbo") //TODO may need to switch to "gpt-3.5-turbo" whilst testing
                .temperature(0.7D) // balanced between randomness and focussed, so a semi-random output
                .build();
    }

    @Bean
    public ChatModel chatModel(OpenAiApi openAiApi, OpenAiChatOptions options) {
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
