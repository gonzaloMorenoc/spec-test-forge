package com.specforge.core.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Objects;

public class OpenAiLlmProvider implements LlmProvider {

    public static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final ChatLanguageModel chatModel;

    public OpenAiLlmProvider() {
        this(resolveApiKey(), resolveModelName(), resolveBaseUrl());
    }

    public OpenAiLlmProvider(String apiKey, String modelName, String baseUrl) {
        String resolvedApiKey = requireNonBlank(apiKey, "apiKey");
        String resolvedModelName = requireNonBlank(modelName, "modelName");

        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(resolvedApiKey)
                .modelName(resolvedModelName);

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        this.chatModel = builder.build();
    }

    @Override
    public String generate(String prompt) {
        String sanitizedPrompt = requireNonBlank(prompt, "prompt");
        return chatModel.generate(sanitizedPrompt);
    }

    private static String resolveApiKey() {
        return firstNonBlank(
                System.getProperty("specforge.llm.openai.apiKey"),
                System.getenv("OPENAI_API_KEY"),
                System.getenv("SPECFORGE_OPENAI_API_KEY")
        );
    }

    private static String resolveModelName() {
        return firstNonBlank(
                System.getProperty("specforge.llm.openai.model"),
                System.getenv("SPECFORGE_OPENAI_MODEL"),
                DEFAULT_MODEL
        );
    }

    private static String resolveBaseUrl() {
        return firstNonBlank(
                System.getProperty("specforge.llm.openai.baseUrl"),
                System.getenv("OPENAI_BASE_URL"),
                System.getenv("SPECFORGE_OPENAI_BASE_URL")
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
