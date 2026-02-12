package com.specforge.core.planner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specforge.core.llm.LlmProvider;
import com.specforge.core.model.OperationModel;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AiScenarioPlanner {

    private static final TypeReference<List<TestScenario>> SCENARIO_LIST_TYPE = new TypeReference<>() {};
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;
    private final Duration timeout;

    public AiScenarioPlanner(LlmProvider llmProvider) {
        this(llmProvider, DEFAULT_TIMEOUT);
    }

    public AiScenarioPlanner(LlmProvider llmProvider, Duration timeout) {
        this.llmProvider = llmProvider;
        this.objectMapper = new ObjectMapper();
        this.timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }

    public List<TestScenario> plan(OperationModel operation) {
        if (llmProvider == null || operation == null) {
            return List.of();
        }

        String prompt = buildPrompt(operation);

        try {
            String response = generateWithTimeout(prompt);
            if (response == null || response.isBlank()) {
                return List.of();
            }

            List<TestScenario> scenarios = objectMapper.readValue(response, SCENARIO_LIST_TYPE);
            return scenarios == null ? List.of() : scenarios;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (JsonProcessingException | ExecutionException | TimeoutException e) {
            return List.of();
        }
    }

    private String buildPrompt(OperationModel operation) {
        String method = safe(operation.getHttpMethod());
        String path = safe(operation.getPath());
        String description = safe(operation.getDescription());

        return "Analyze this API Endpoint: " + method + " " + path
                + " with description " + description
                + ". List 5 distinct test scenarios focusing on security, boundary values, and happy path. "
                + "Return the list in JSON format: [{name, description, expectedStatus}].";
    }

    private String generateWithTimeout(String prompt)
            throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> llmProvider.generate(prompt));
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
