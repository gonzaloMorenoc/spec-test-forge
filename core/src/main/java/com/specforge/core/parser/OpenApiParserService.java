package com.specforge.core.parser;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.ParamLocation;
import com.specforge.core.model.ParamModel;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class OpenApiParserService {

    public ApiSpecModel parse(String specLocation) {
        String resolvedLocation = resolveLocation(specLocation);

        SwaggerParseResult result = new OpenAPIV3Parser().readLocation(resolvedLocation, null, null);

        if (result == null || result.getOpenAPI() == null) {
            String msg = (result != null && result.getMessages() != null)
                    ? String.join("\n", result.getMessages())
                    : "Unknown parsing error";
            throw new IllegalArgumentException("Failed to parse OpenAPI spec: " + msg);
        }

        OpenAPI api = result.getOpenAPI();

        ApiSpecModel model = new ApiSpecModel();
        model.setTitle(api.getInfo() != null ? api.getInfo().getTitle() : "API");
        model.setVersion(api.getInfo() != null ? api.getInfo().getVersion() : "unknown");

        List<OperationModel> ops = new ArrayList<>();
        if (api.getPaths() != null) {
            for (Map.Entry<String, PathItem> e : api.getPaths().entrySet()) {
                String path = e.getKey();
                PathItem item = e.getValue();

                List<Parameter> pathParameters = item.getParameters();
                addOperationIfPresent(ops, "GET", path, pathParameters, item.getGet());
                addOperationIfPresent(ops, "POST", path, pathParameters, item.getPost());
                addOperationIfPresent(ops, "PUT", path, pathParameters, item.getPut());
                addOperationIfPresent(ops, "PATCH", path, pathParameters, item.getPatch());
                addOperationIfPresent(ops, "DELETE", path, pathParameters, item.getDelete());
                addOperationIfPresent(ops, "HEAD", path, pathParameters, item.getHead());
                addOperationIfPresent(ops, "OPTIONS", path, pathParameters, item.getOptions());
                addOperationIfPresent(ops, "TRACE", path, pathParameters, item.getTrace());
            }
        }

        model.setOperations(ops);
        return model;
    }

    private String resolveLocation(String specLocation) {
        // If it's already a URL/URI, keep it.
        String lower = specLocation.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) {
            return specLocation;
        }

        // Treat as filesystem path (relative or absolute).
        Path p = Path.of(specLocation).toAbsolutePath().normalize();
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("Spec file not found: " + p);
        }

        // Convert to file:// URI so swagger-parser won't try classpath.
        return p.toUri().toString();
    }

    private void addOperationIfPresent(List<OperationModel> ops, String method, String path, List<Parameter> pathParameters, Operation op) {
        if (op == null) return;

        OperationModel om = new OperationModel();
        om.setHttpMethod(method);
        om.setPath(path);

        String operationId = Optional.ofNullable(op.getOperationId())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> defaultOperationId(method, path));

        om.setOperationId(operationId);
        om.setPreferredSuccessStatus(preferredSuccessStatus(op.getResponses()));
        om.setParams(extractParams(pathParameters, op.getParameters()));

        if (op.getTags() != null) {
            om.setTags(new ArrayList<>(op.getTags()));
        }

        ops.add(om);
    }

    private String defaultOperationId(String method, String path) {
        String normalized = path.replaceAll("\\{", "")
                .replaceAll("}", "")
                .replaceAll("[^a-zA-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toLowerCase();
        return method.toLowerCase() + "_" + normalized;
    }

    private int preferredSuccessStatus(ApiResponses responses) {
        if (responses == null || responses.isEmpty()) {
            return 200;
        }

        Set<Integer> parsedStatuses = new TreeSet<>();
        Set<Integer> successStatuses = new TreeSet<>();
        for (String code : responses.keySet()) {
            OptionalInt status = parseStatusCode(code);
            if (status.isEmpty()) {
                continue;
            }

            int value = status.getAsInt();
            parsedStatuses.add(value);
            if (value >= 200 && value < 300) {
                successStatuses.add(value);
            }
        }

        if (!successStatuses.isEmpty()) {
            int[] preferredOrder = {201, 200, 204, 202};
            for (int candidate : preferredOrder) {
                if (successStatuses.contains(candidate)) {
                    return candidate;
                }
            }
            return successStatuses.iterator().next();
        }

        if (!parsedStatuses.isEmpty()) {
            return parsedStatuses.iterator().next();
        }

        return 200;
    }

    private OptionalInt parseStatusCode(String raw) {
        if (raw == null) {
            return OptionalInt.empty();
        }

        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (code.matches("\\d{3}")) {
            return OptionalInt.of(Integer.parseInt(code));
        }

        if (code.matches("[1-5]XX")) {
            int family = Character.digit(code.charAt(0), 10);
            return OptionalInt.of(family * 100);
        }

        return OptionalInt.empty();
    }

    private List<ParamModel> extractParams(List<Parameter> pathParameters, List<Parameter> operationParameters) {
        LinkedHashMap<String, ParamModel> ordered = new LinkedHashMap<>();

        addParams(ordered, pathParameters);
        addParams(ordered, operationParameters);

        return new ArrayList<>(ordered.values());
    }

    private void addParams(Map<String, ParamModel> collector, List<Parameter> parameters) {
        if (parameters == null) {
            return;
        }

        for (Parameter parameter : parameters) {
            if (parameter == null || parameter.getName() == null || parameter.getName().isBlank()) {
                continue;
            }

            ParamLocation location = mapLocation(parameter.getIn());
            if (location == null) {
                continue;
            }

            ParamModel model = new ParamModel();
            model.setName(parameter.getName());
            model.setIn(location);
            model.setRequired(Boolean.TRUE.equals(parameter.getRequired()) || location == ParamLocation.PATH);
            model.setType(extractType(parameter.getSchema()));

            collector.put(paramKey(location, model.getName()), model);
        }
    }

    private ParamLocation mapLocation(String raw) {
        if (raw == null) {
            return null;
        }

        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "path" -> ParamLocation.PATH;
            case "query" -> ParamLocation.QUERY;
            case "header" -> ParamLocation.HEADER;
            default -> null;
        };
    }

    private String extractType(Schema<?> schema) {
        if (schema == null || schema.getType() == null || schema.getType().isBlank()) {
            return "string";
        }
        return schema.getType().toLowerCase(Locale.ROOT);
    }

    private String paramKey(ParamLocation location, String name) {
        return location + ":" + name.toLowerCase(Locale.ROOT);
    }
}
