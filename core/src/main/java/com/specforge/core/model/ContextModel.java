package com.specforge.core.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContextModel {

    private Map<String, List<String>> rulesByEndpointPath = new LinkedHashMap<>();

    public Map<String, List<String>> getRulesByEndpointPath() {
        return rulesByEndpointPath;
    }

    public void setRulesByEndpointPath(Map<String, List<String>> rulesByEndpointPath) {
        this.rulesByEndpointPath = rulesByEndpointPath == null ? new LinkedHashMap<>() : rulesByEndpointPath;
    }

    public List<String> getRulesForPath(String path) {
        if (path == null || path.isBlank() || rulesByEndpointPath.isEmpty()) {
            return List.of();
        }

        List<String> direct = rulesByEndpointPath.get(path);
        if (direct != null && !direct.isEmpty()) {
            return List.copyOf(direct);
        }

        String normalized = normalizePath(path);
        for (Map.Entry<String, List<String>> entry : rulesByEndpointPath.entrySet()) {
            if (normalizePath(entry.getKey()).equals(normalized)) {
                return List.copyOf(entry.getValue());
            }
        }

        return List.of();
    }

    public void addRule(String endpointPath, String rule) {
        if (endpointPath == null || endpointPath.isBlank() || rule == null || rule.isBlank()) {
            return;
        }

        rulesByEndpointPath
                .computeIfAbsent(endpointPath.trim(), ignored -> new ArrayList<>())
                .add(rule.trim());
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String trimmed = path.trim();
        if (trimmed.length() > 1 && trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
