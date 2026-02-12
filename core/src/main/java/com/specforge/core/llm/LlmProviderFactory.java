package com.specforge.core.llm;

import java.util.Locale;

public final class LlmProviderFactory {

    public static final String PROVIDER_OLLAMA = "OLLAMA";
    public static final String PROVIDER_OPENAI = "OPENAI";

    private LlmProviderFactory() {
    }

    public static LlmProvider createFromConfig() {
        return create(resolveProviderName());
    }

    public static LlmProvider create(String providerName) {
        String normalized = normalizeProviderName(providerName);
        return switch (normalized) {
            case PROVIDER_OLLAMA -> new OllamaLlmProvider();
            case PROVIDER_OPENAI -> new OpenAiLlmProvider();
            default -> throw new IllegalArgumentException(
                    "Unsupported LLM provider: " + providerName + ". Supported values: OLLAMA, OPENAI"
            );
        };
    }

    private static String resolveProviderName() {
        String configured = firstNonBlank(
                System.getProperty("specforge.llm.provider"),
                System.getenv("LLM_PROVIDER"),
                System.getenv("SPECFORGE_LLM_PROVIDER")
        );
        return normalizeProviderName(configured == null ? PROVIDER_OLLAMA : configured);
    }

    private static String normalizeProviderName(String providerName) {
        return providerName == null ? "" : providerName.trim().toUpperCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
