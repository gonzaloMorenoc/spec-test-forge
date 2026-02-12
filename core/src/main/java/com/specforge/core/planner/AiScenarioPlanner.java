package com.specforge.core.planner;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specforge.core.llm.LlmProvider;
import com.specforge.core.model.ContextModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.prompt.PromptManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AiScenarioPlanner {

    private static final TypeReference<List<TestScenario>> SCENARIO_LIST_TYPE = new TypeReference<>() {};
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final String TEMPLATE_NAME = "ai-scenario-planner";
    private static final int DEFAULT_SCENARIO_COUNT = 5;
    private static final String DEFAULT_RULES = "Prioritize practical API test coverage and avoid duplicate scenarios.";

    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;
    private final PromptManager promptManager;
    private final ContextModel contextModel;
    private final Duration timeout;

    public AiScenarioPlanner(LlmProvider llmProvider) {
        this(llmProvider, new PromptManager(), new ContextModel(), DEFAULT_TIMEOUT);
    }

    public AiScenarioPlanner(LlmProvider llmProvider, Duration timeout) {
        this(llmProvider, new PromptManager(), new ContextModel(), timeout);
    }

    public AiScenarioPlanner(LlmProvider llmProvider, ContextModel contextModel) {
        this(llmProvider, new PromptManager(), contextModel, DEFAULT_TIMEOUT);
    }

    public AiScenarioPlanner(LlmProvider llmProvider, PromptManager promptManager, Duration timeout) {
        this(llmProvider, promptManager, new ContextModel(), timeout);
    }

    public AiScenarioPlanner(LlmProvider llmProvider,
                             PromptManager promptManager,
                             ContextModel contextModel,
                             Duration timeout) {
        this.llmProvider = llmProvider;
        this.objectMapper = new ObjectMapper();
        this.promptManager = promptManager != null ? promptManager : new PromptManager();
        this.contextModel = contextModel != null ? contextModel : new ContextModel();
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
        String businessRules = formatBusinessRules(path);

        return promptManager.render(
                TEMPLATE_NAME,
                Map.of(
                        "httpMethod", method,
                        "path", path,
                        "description", description,
                        "businessRules", businessRules,
                        "scenarioCount", DEFAULT_SCENARIO_COUNT,
                        "rules", resolveRules()
                )
        );
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

    private String resolveRules() {
        return firstNonBlank(
                System.getProperty("specforge.prompts.aiScenario.rules"),
                System.getenv("SPECFORGE_PROMPT_AI_SCENARIO_RULES"),
                DEFAULT_RULES
        );
    }

    private String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private String formatBusinessRules(String path) {
        List<String> rules = contextModel.getRulesForPath(path);
        if (rules.isEmpty()) {
            return "- No additional business rules provided.";
        }

        StringBuilder sb = new StringBuilder();
        for (String rule : rules) {
            sb.append("- ").append(rule).append('\n');
        }
        return sb.toString().trim();
    }
}
