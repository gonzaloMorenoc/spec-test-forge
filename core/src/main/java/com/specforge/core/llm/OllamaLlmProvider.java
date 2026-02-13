package com.specforge.core.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.util.Objects;

public class OllamaLlmProvider implements LlmProvider {

    public static final String DEFAULT_BASE_URL = "http://localhost:11434";
    public static final String DEFAULT_MODEL = "kimi-k2.5:cloud";

    private final ChatLanguageModel chatModel;

    public OllamaLlmProvider() {
        this(resolveBaseUrl(), resolveModelName());
    }

    public OllamaLlmProvider(String baseUrl, String modelName) {
        String resolvedBaseUrl = requireNonBlank(baseUrl, "baseUrl");
        String resolvedModelName = requireNonBlank(modelName, "modelName");

        this.chatModel = OllamaChatModel.builder()
                .baseUrl(resolvedBaseUrl)
                .modelName(resolvedModelName)
                .build();
    }

    @Override
    public String generate(String prompt) {
        String sanitizedPrompt = requireNonBlank(prompt, "prompt");
        return chatModel.generate(sanitizedPrompt);
    }

    private static String resolveBaseUrl() {
        return firstNonBlank(
                System.getProperty("specforge.llm.ollama.baseUrl"),
                System.getenv("SPECFORGE_OLLAMA_BASE_URL"),
                DEFAULT_BASE_URL
        );
    }

    private static String resolveModelName() {
        return firstNonBlank(
                System.getProperty("specforge.llm.ollama.model"),
                System.getenv("SPECFORGE_OLLAMA_MODEL"),
                DEFAULT_MODEL
        );
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
