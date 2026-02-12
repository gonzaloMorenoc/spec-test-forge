package com.specforge.core.parser;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SchemaResolver {

    private final Map<String, Schema> componentSchemas;

    public SchemaResolver(OpenAPI api) {
        if (api != null && api.getComponents() != null && api.getComponents().getSchemas() != null) {
            this.componentSchemas = api.getComponents().getSchemas();
        } else {
            this.componentSchemas = Map.of();
        }
    }

    public Map<String, Object> resolveSchema(Schema<?> schema) {
        if (schema == null) {
            return null;
        }
        return toMap(schema, new TreeSet<>());
    }

    private Map<String, Object> toMap(Schema<?> schema, Set<String> resolvingRefs) {
        if (schema == null) {
            return Map.of("type", "object");
        }

        if (schema.get$ref() != null && !schema.get$ref().isBlank()) {
            String refName = refName(schema.get$ref());
            if (!resolvingRefs.add(refName)) {
                return Map.of("type", "object");
            }

            Schema<?> resolved = componentSchemas.get(refName);
            Map<String, Object> map = resolved == null
                    ? Map.of("type", "object")
                    : toMap(resolved, resolvingRefs);
            resolvingRefs.remove(refName);
            return map;
        }

        Map<String, Object> out = new LinkedHashMap<>();

        String type = normalizeType(schema);
        if (type != null) {
            out.put("type", type);
        }

        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            out.put("enum", new ArrayList<>(schema.getEnum()));
        }

        if (schema.getFormat() != null && !schema.getFormat().isBlank()) {
            out.put("format", schema.getFormat());
        }

        if (schema.getMinLength() != null) {
            out.put("minLength", schema.getMinLength());
        }
        if (schema.getMaxLength() != null) {
            out.put("maxLength", schema.getMaxLength());
        }
        if (schema.getMinimum() != null) {
            out.put("minimum", asNumber(schema.getMinimum()));
        }
        if (schema.getMaximum() != null) {
            out.put("maximum", asNumber(schema.getMaximum()));
        }
        if (schema.getMinItems() != null) {
            out.put("minItems", schema.getMinItems());
        }
        if (schema.getMaxItems() != null) {
            out.put("maxItems", schema.getMaxItems());
        }

        if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
            out.put("required", new ArrayList<>(schema.getRequired()));
        }

        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                properties.put(entry.getKey(), toMap(entry.getValue(), resolvingRefs));
            }
            out.put("properties", properties);
        }

        Schema<?> items = schema instanceof ArraySchema
                ? ((ArraySchema) schema).getItems()
                : schema.getItems();
        if (items != null) {
            out.put("items", toMap(items, resolvingRefs));
        }

        if (schema.getAllOf() != null && !schema.getAllOf().isEmpty()) {
            List<Object> allOf = new ArrayList<>();
            for (Schema<?> s : schema.getAllOf()) {
                allOf.add(toMap(s, resolvingRefs));
            }
            out.put("allOf", allOf);
        }
        if (schema.getAnyOf() != null && !schema.getAnyOf().isEmpty()) {
            List<Object> anyOf = new ArrayList<>();
            for (Schema<?> s : schema.getAnyOf()) {
                anyOf.add(toMap(s, resolvingRefs));
            }
            out.put("anyOf", anyOf);
        }
        if (schema.getOneOf() != null && !schema.getOneOf().isEmpty()) {
            List<Object> oneOf = new ArrayList<>();
            for (Schema<?> s : schema.getOneOf()) {
                oneOf.add(toMap(s, resolvingRefs));
            }
            out.put("oneOf", oneOf);
        }

        return out;
    }

    private String normalizeType(Schema<?> schema) {
        if (schema == null) {
            return "object";
        }

        if (schema.getType() != null && !schema.getType().isBlank()) {
            return schema.getType().toLowerCase(Locale.ROOT);
        }
        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            return "object";
        }
        if (schema instanceof ArraySchema || schema.getItems() != null) {
            return "array";
        }
        return null;
    }

    private String refName(String rawRef) {
        int idx = rawRef.lastIndexOf('/');
        return idx >= 0 ? rawRef.substring(idx + 1) : rawRef;
    }

    private Number asNumber(BigDecimal value) {
        if (value.scale() <= 0) {
            return value.longValue();
        }
        return value.doubleValue();
    }
}
