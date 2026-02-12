package com.specforge.core.prompt;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.StringWriter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PromptManager {

    private static final String PROMPTS_BASE_PATH = "prompts";
    private final MustacheFactory mustacheFactory;
    private final ConcurrentMap<String, Mustache> cache;

    public PromptManager() {
        this(new DefaultMustacheFactory(), new ConcurrentHashMap<>());
    }

    PromptManager(MustacheFactory mustacheFactory, ConcurrentMap<String, Mustache> cache) {
        this.mustacheFactory = Objects.requireNonNull(mustacheFactory, "mustacheFactory must not be null");
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    public String render(String templateName, Map<String, Object> variables) {
        String normalizedTemplateName = requireNonBlank(templateName, "templateName");
        Map<String, Object> context = variables == null ? Map.of() : variables;
        Mustache mustache = cache.computeIfAbsent(normalizedTemplateName, this::compileTemplate);
        StringWriter writer = new StringWriter();
        mustache.execute(writer, context);
        return writer.toString().trim();
    }

    private Mustache compileTemplate(String templateName) {
        String resourcePath = PROMPTS_BASE_PATH + "/" + templateName + ".mustache";
        Mustache compiled = mustacheFactory.compile(resourcePath);
        if (compiled == null) {
            throw new IllegalStateException("Prompt template not found: " + resourcePath);
        }
        return compiled;
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
