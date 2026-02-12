package com.specforge.core.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specforge.core.model.ContextModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ContextParserService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContextModel parse(Path file) {
        if (file == null) {
            return new ContextModel();
        }

        try {
            String content = Files.readString(file);
            String fileName = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (fileName.endsWith(".json")) {
                return parseJson(content);
            }
            return parseMarkdown(content);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read context file: " + file, e);
        }
    }

    private ContextModel parseJson(String content) {
        try {
            Map<String, Object> root = objectMapper.readValue(content, MAP_TYPE);
            ContextModel model = new ContextModel();

            Object nested = root.get("rulesByEndpointPath");
            if (nested instanceof Map<?, ?> nestedMap) {
                populateFromMap(model, castStringObjectMap(nestedMap));
                return model;
            }

            populateFromMap(model, root);
            return model;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON context content", e);
        }
    }

    private ContextModel parseMarkdown(String content) {
        ContextModel model = new ContextModel();
        String currentPath = null;
        for (String rawLine : content.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isBlank()) {
                continue;
            }

            String headerPath = extractHeaderPath(line);
            if (headerPath != null) {
                currentPath = headerPath;
                continue;
            }

            int colonIndex = line.indexOf(':');
            if (line.startsWith("-") && colonIndex > 1) {
                String maybePath = line.substring(1, colonIndex).trim();
                String maybeRule = line.substring(colonIndex + 1).trim();
                if (looksLikePath(maybePath) && !maybeRule.isBlank()) {
                    model.addRule(maybePath, maybeRule);
                    continue;
                }
            }

            if ((line.startsWith("- ") || line.startsWith("* ")) && currentPath != null) {
                model.addRule(currentPath, line.substring(2).trim());
            }
        }
        return model;
    }

    private void populateFromMap(ContextModel model, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String endpointPath = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String s) {
                model.addRule(endpointPath, s);
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        model.addRule(endpointPath, String.valueOf(item));
                    }
                }
            }
        }
    }

    private String extractHeaderPath(String line) {
        if (!line.startsWith("#")) {
            return null;
        }
        String headerText = line.replaceFirst("^#+\\s*", "").trim();
        return looksLikePath(headerText) ? headerText : null;
    }

    private boolean looksLikePath(String value) {
        return value != null && value.startsWith("/") && value.contains("/");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castStringObjectMap(Map<?, ?> map) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null) {
                out.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return out;
    }
}
