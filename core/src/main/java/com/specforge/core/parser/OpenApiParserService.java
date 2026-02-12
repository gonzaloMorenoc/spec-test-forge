package com.specforge.core.parser;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
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

                addOperationIfPresent(ops, "GET", path, item.getGet());
                addOperationIfPresent(ops, "POST", path, item.getPost());
                addOperationIfPresent(ops, "PUT", path, item.getPut());
                addOperationIfPresent(ops, "PATCH", path, item.getPatch());
                addOperationIfPresent(ops, "DELETE", path, item.getDelete());
                addOperationIfPresent(ops, "HEAD", path, item.getHead());
                addOperationIfPresent(ops, "OPTIONS", path, item.getOptions());
                addOperationIfPresent(ops, "TRACE", path, item.getTrace());
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

    private void addOperationIfPresent(List<OperationModel> ops, String method, String path, Operation op) {
        if (op == null) return;

        OperationModel om = new OperationModel();
        om.setHttpMethod(method);
        om.setPath(path);

        String operationId = Optional.ofNullable(op.getOperationId())
                .filter(s -> !s.isBlank())
                .orElseGet(() -> defaultOperationId(method, path));

        om.setOperationId(operationId);

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
}