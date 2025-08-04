package com.richieloco.coinsniper.service.risk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.richieloco.coinsniper.config.AiPromptConfig;
import com.richieloco.coinsniper.entity.ExchangeAssessmentRecord;
import com.richieloco.coinsniper.model.ExchangeAssessmentResponse;
import com.richieloco.coinsniper.repository.ExchangeAssessmentRepository;
import com.richieloco.coinsniper.service.risk.context.BaseAssessor;
import com.richieloco.coinsniper.service.risk.context.ExchangeSelectorContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ExchangeAssessor extends BaseAssessor<ExchangeSelectorContext, List<ExchangeAssessmentRecord>> {

    private final ExchangeAssessmentRepository repository;
    private final AiPromptConfig aiPromptConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExchangeAssessor(ChatModel chatModel, ExchangeAssessmentRepository repository, AiPromptConfig aiPromptConfig) {
        super(chatModel);
        this.repository = repository;
        this.aiPromptConfig = aiPromptConfig;
    }

    @Override
    protected String generatePrompt(ExchangeSelectorContext context) {
        PromptTemplate prompt = aiPromptConfig.exchangeCoinAvailabilityPromptTemplate();
        if (prompt == null) {
            throw new NullPointerException("PromptTemplate is null for ExchangeAssessor");
        }

        // Get raw template string
        String template = prompt.getTemplate();

        // Manually replace placeholders to avoid {var} parsing issues
        return template
                .replace("<exchanges>", context.exchanges())
                .replace("<targetCoin>", context.targetCoin())
                .replace("<stableCoins>", context.stableCoins());
    }

    @Override
    protected String contextType() {
        return "Exchange";
    }

    @Override
    protected List<ExchangeAssessmentRecord> parseAssessmentOutput(ExchangeSelectorContext context, String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new NullPointerException("LLM response was null or empty");
        }
        log.debug("LLM response: '{}'", response);

        try {
            // Try JSON
            if (response.trim().startsWith("[")) {
                List<ExchangeAssessmentResponse> responses = objectMapper.readValue(
                        response, new TypeReference<>() {});
                return responses.stream()
                        .map(dto -> buildRecord(context, dto.exchange(), dto.coinListing(),
                                dto.overallRiskScore(), dto.liquidity(), dto.tradingVolume(), dto.tradingFees()))
                        .toList();
            }

            // Fallback to key-value parsing
            Map<String, String> values = Arrays.stream(response.split(","))
                    .map(String::trim)
                    .map(s -> s.split(":", 2))
                    .filter(arr -> arr.length == 2)
                    .collect(Collectors.toMap(
                            arr -> arr[0].trim(),
                            arr -> arr[1].trim(),
                            (a, b) -> a // handle duplicates gracefully
                    ));

            // Validate minimal required keys
            if (!values.containsKey("Exchange") || !values.containsKey("Coin Listing") || !values.containsKey("Overall Risk Score")) {
                throw new RuntimeException("Failed to map LLM response: " + response);
            }

            return List.of(buildRecord(
                    context,
                    values.get("Exchange"),
                    values.get("Coin Listing"),
                    values.get("Overall Risk Score"),
                    values.getOrDefault("Liquidity", "Medium"),
                    values.getOrDefault("Trading Volume", "Medium"),
                    values.getOrDefault("Trading Fees", "Medium")
            ));

        } catch (RuntimeException e) {
            throw e; // Preserve our own error message
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response: " + response, e);
        }
    }

    private ExchangeAssessmentRecord buildRecord(ExchangeSelectorContext context,
                                                 String exchange,
                                                 String coinListing,
                                                 String riskScore,
                                                 String liquidity,
                                                 String tradingVolume,
                                                 String tradingFees) {
        return ExchangeAssessmentRecord.builder()
                .contextType(contextType())
                .contextDescription(context.toString())
                .exchange(exchange)
                .coinListing(coinListing)
                .overallRiskScore(riskScore)
                .liquidity(liquidity)
                .tradingVolume(tradingVolume)
                .tradingFees(tradingFees)
                .assessedAt(Instant.now())
                .build();
    }

    @Override
    protected void logAssessment(ExchangeSelectorContext context, List<ExchangeAssessmentRecord> records) {
        repository.saveAll(records)
                .onErrorContinue((err, obj) -> log.warn("Save failed: {}", err.getMessage()))
                .subscribe();
    }

    @Override
    public Mono<List<ExchangeAssessmentRecord>> assess(ExchangeSelectorContext context) {
        return generateAssessment(context)
                .map(response -> {
                    if (response == null || response.trim().isEmpty()) {
                        throw new NullPointerException("LLM returned null or empty generation text");
                    }
                    return parseAssessmentOutput(context, response);
                })
                .flatMapMany(Flux::fromIterable)
                .flatMap(record -> repository.save(record)
                        .onErrorResume(err -> {
                            log.warn("Repository save failed for {}: {}", record.getExchange(), err.getMessage());
                            return Mono.just(record);
                        })
                )
                .collectList();
    }
}
