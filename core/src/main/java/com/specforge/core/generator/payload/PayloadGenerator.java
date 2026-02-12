package com.specforge.core.generator.payload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specforge.core.llm.LlmProvider;
import com.specforge.core.prompt.PromptManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class PayloadGenerator {

    private static final Duration DEFAULT_LLM_TIMEOUT = Duration.ofSeconds(10);
    private static final String TEMPLATE_NAME = "payload-generator";
    private static final String DEFAULT_RULES = "Respect types, enum values, and all required fields.";

    private final Random random;
    private final ObjectMapper objectMapper;
    private final LlmProvider llmProvider;
    private final PromptManager promptManager;
    private final Duration llmTimeout;

    public PayloadGenerator() {
        this(1234L, null);
    }

    public PayloadGenerator(long seed) {
        this(seed, null);
    }

    public PayloadGenerator(long seed, LlmProvider llmProvider) {
        this(seed, llmProvider, DEFAULT_LLM_TIMEOUT);
    }

    public PayloadGenerator(long seed, LlmProvider llmProvider, Duration llmTimeout) {
        this(seed, llmProvider, new PromptManager(), llmTimeout);
    }

    public PayloadGenerator(long seed, LlmProvider llmProvider, PromptManager promptManager, Duration llmTimeout) {
        this.random = new Random(seed);
        this.objectMapper = new ObjectMapper();
        this.llmProvider = llmProvider;
        this.promptManager = promptManager != null ? promptManager : new PromptManager();
        this.llmTimeout = llmTimeout != null ? llmTimeout : DEFAULT_LLM_TIMEOUT;
    }

    public Object generate(Map<String, Object> schema) {
        if (schema == null || schema.isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        return generateBySchema(schema);
    }

    private Object generateBySchema(Map<String, Object> schema) {
        Object llmGenerated = generateByLlm(schema);
        if (llmGenerated != null) {
            return llmGenerated;
        }
        return generateBySchemaRandom(schema);
    }

    private Object generateByLlm(Map<String, Object> schema) {
        if (llmProvider == null) {
            return null;
        }

        try {
            String schemaJson = objectMapper.writeValueAsString(schema);
            String prompt = promptManager.render(
                    TEMPLATE_NAME,
                    Map.of(
                            "schemaJson", schemaJson,
                            "rules", resolveRules()
                    )
            );

            String rawResponse = generateWithTimeout(prompt);
            if (rawResponse == null || rawResponse.isBlank()) {
                return null;
            }

            return objectMapper.readValue(rawResponse, Object.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (JsonProcessingException | ExecutionException | TimeoutException e) {
            return null;
        }
    }

    private String generateWithTimeout(String prompt)
            throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> llmProvider.generate(prompt));
        try {
            return future.get(llmTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } finally {
            future.cancel(true);
        }
    }

    private Object generateBySchemaRandom(Map<String, Object> schema) {
        List<?> enumValues = list(schema.get("enum"));
        if (!enumValues.isEmpty()) {
            return enumValues.getFirst();
        }

        String type = asString(schema.get("type")).toLowerCase(Locale.ROOT);
        return switch (type) {
            case "object" -> generateObject(schema);
            case "array" -> generateArray(schema);
            case "integer" -> generateInteger(schema);
            case "number" -> generateNumber(schema);
            case "boolean" -> Boolean.TRUE;
            case "string" -> generateString(schema);
            default -> generateFallback(schema);
        };
    }

    private Map<String, Object> generateObject(Map<String, Object> schema) {
        Map<String, Object> out = new LinkedHashMap<>();

        Map<String, Object> properties = map(schema.get("properties"));
        List<String> required = toStringList(schema.get("required"));

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            String propertyName = entry.getKey();
            Map<String, Object> propertySchema = map(entry.getValue());

            if (!required.contains(propertyName)) {
                // Deterministic but seedable optional inclusion for future extensibility.
                if (random.nextBoolean()) {
                    out.put(propertyName, generateBySchemaRandom(propertySchema));
                }
            } else {
                out.put(propertyName, generateBySchemaRandom(propertySchema));
            }
        }

        return out;
    }

    private List<Object> generateArray(Map<String, Object> schema) {
        int minItems = asInt(schema.get("minItems"), 1);
        int maxItems = asInt(schema.get("maxItems"), Math.max(minItems, 1));
        int size = clamp(1, minItems, maxItems);

        Map<String, Object> itemSchema = map(schema.get("items"));
        List<Object> out = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            out.add(generateBySchemaRandom(itemSchema));
        }
        return out;
    }

    private Long generateInteger(Map<String, Object> schema) {
        long minimum = asLong(schema.get("minimum"), 1L);
        long maximum = asLong(schema.get("maximum"), Math.max(minimum, 1L));
        return clamp(1L, minimum, maximum);
    }

    private Double generateNumber(Map<String, Object> schema) {
        double minimum = asDouble(schema.get("minimum"), 1.0);
        double maximum = asDouble(schema.get("maximum"), Math.max(minimum, 1.0));
        return clamp(1.0, minimum, maximum);
    }

    private String generateString(Map<String, Object> schema) {
        String format = asString(schema.get("format")).toLowerCase(Locale.ROOT);
        String base = switch (format) {
            case "email" -> "user@example.com";
            case "uuid" -> "00000000-0000-4000-8000-000000000000";
            case "date-time" -> "2025-01-01T00:00:00Z";
            case "date" -> "2025-01-01";
            default -> "value";
        };

        int minLength = asInt(schema.get("minLength"), 0);
        int maxLength = asInt(schema.get("maxLength"), Integer.MAX_VALUE);

        String adjusted = ensureMinLength(base, minLength);
        if (adjusted.length() > maxLength) {
            adjusted = adjusted.substring(0, Math.max(0, maxLength));
        }
        if (adjusted.isEmpty()) {
            adjusted = "a";
        }
        return adjusted;
    }

    private Object generateFallback(Map<String, Object> schema) {
        if (schema.containsKey("properties")) {
            return generateObject(schema);
        }
        if (schema.containsKey("items")) {
            return generateArray(schema);
        }
        return "value";
    }

    private String ensureMinLength(String source, int minLength) {
        if (source.length() >= minLength) {
            return source;
        }

        StringBuilder sb = new StringBuilder(source);
        while (sb.length() < minLength) {
            sb.append('a');
        }
        return sb.toString();
    }

    private long clamp(long preferred, long min, long max) {
        if (min > max) {
            return min;
        }
        return Math.max(min, Math.min(max, preferred));
    }

    private int clamp(int preferred, int min, int max) {
        if (min > max) {
            return min;
        }
        return Math.max(min, Math.min(max, preferred));
    }

    private double clamp(double preferred, double min, double max) {
        if (min > max) {
            return min;
        }
        return Math.max(min, Math.min(max, preferred));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        if (value instanceof List<?> l) {
            return (List<Object>) l;
        }
        return List.of();
    }

    private List<String> toStringList(Object value) {
        List<Object> raw = list(value);
        List<String> out = new ArrayList<>();
        for (Object o : raw) {
            out.add(String.valueOf(o));
        }
        return out;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return fallback;
    }

    private long asLong(Object value, long fallback) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return fallback;
    }

    private double asDouble(Object value, double fallback) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return fallback;
    }

    private String resolveRules() {
        return firstNonBlank(
                System.getProperty("specforge.prompts.payload.rules"),
                System.getenv("SPECFORGE_PROMPT_PAYLOAD_RULES"),
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
}
