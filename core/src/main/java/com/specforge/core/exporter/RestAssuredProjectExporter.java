package com.specforge.core.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.specforge.core.generator.payload.PayloadGenerator;
import com.specforge.core.llm.LlmProvider;
import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.ParamLocation;
import com.specforge.core.model.ParamModel;
import com.specforge.core.model.TestCaseModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestAssuredProjectExporter {

    private static final long LLM_TIMEOUT_SECONDS = 20;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PayloadGenerator payloadGenerator = new PayloadGenerator(1234L);
    private final LlmProvider llmProvider;

    public RestAssuredProjectExporter() {
        this(null);
    }

    public RestAssuredProjectExporter(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public void export(ApiSpecModel model,
                       Path outputDir,
                       String basePackage,
                       GenerationMode mode,
                       String baseUrl) {

        try {
            Files.createDirectories(outputDir);

            if (mode == GenerationMode.STANDALONE) {
                writeStandaloneGradleProject(outputDir);
            }

            Path testJavaRoot = outputDir.resolve("src/test/java");
            Path testResRoot = outputDir.resolve("src/test/resources");
            Files.createDirectories(testJavaRoot);
            Files.createDirectories(testResRoot);

            writeBaseTestConfig(testResRoot, baseUrl);
            Map<String, String> schemaByOperationId = writeResponseSchemas(model.getOperations(), testResRoot);

            Map<String, List<OperationModel>> byTag = groupByPrimaryTag(model.getOperations());
            for (Map.Entry<String, List<OperationModel>> entry : byTag.entrySet()) {
                String tag = entry.getKey();
                String className = toPascalCase(tag) + "ApiTest";
                String java = renderTestClass(basePackage, className, entry.getValue(), schemaByOperationId);

                Path pkgDir = testJavaRoot.resolve(basePackage.replace('.', '/'));
                Files.createDirectories(pkgDir);

                Files.writeString(
                        pkgDir.resolve(className + ".java"),
                        java,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                );
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to export tests: " + e.getMessage(), e);
        }
    }

    private void writeStandaloneGradleProject(Path outputDir) throws IOException {
        Files.writeString(
                outputDir.resolve("settings.gradle"),
                "rootProject.name = \"generated-api-tests\"\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        String buildGradle = """
                plugins {
                    id 'java'
                }

                repositories {
                    mavenCentral()
                }

                dependencies {
                    testImplementation platform('org.junit:junit-bom:5.10.2')
                    testImplementation 'org.junit.jupiter:junit-jupiter'
                    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
                    testImplementation 'io.rest-assured:rest-assured:5.5.0'
                    testImplementation 'io.rest-assured:json-schema-validator:5.5.0'
                }

                test {
                    useJUnitPlatform()
                }
                """;

        Files.writeString(
                outputDir.resolve("build.gradle"),
                buildGradle,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void writeBaseTestConfig(Path testResRoot, String baseUrl) throws IOException {
        String content = "baseUrl=" + (baseUrl == null ? "http://localhost:8080" : baseUrl) + "\n";
        Files.writeString(
                testResRoot.resolve("specforge.properties"),
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private Map<String, List<OperationModel>> groupByPrimaryTag(List<OperationModel> ops) {
        Map<String, List<OperationModel>> map = new LinkedHashMap<>();
        for (OperationModel op : ops) {
            String tag = (op.getTags() != null && !op.getTags().isEmpty()) ? op.getTags().get(0) : "default";
            map.computeIfAbsent(tag, k -> new ArrayList<>()).add(op);
        }
        return map;
    }

    private String renderTestClass(String basePackage,
                                   String className,
                                   List<OperationModel> ops,
                                   Map<String, String> schemaByOperationId) {
        StringBuilder methods = new StringBuilder();
        boolean needsSchemaAssertionImport = false;

        for (OperationModel op : ops) {
            for (TestCaseModel tc : op.getTestCases()) {
                String schemaResource = schemaByOperationId.get(op.getOperationId());
                methods.append(renderTestMethod(op, tc, schemaResource)).append("\n");
                if (schemaResource != null) {
                    needsSchemaAssertionImport = true;
                }
            }
        }

        String schemaImport = needsSchemaAssertionImport
                ? "import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;\n"
                : "";

        return """
                package %s;

                import io.restassured.RestAssured;
                import io.restassured.http.ContentType;
                import io.restassured.specification.RequestSpecification;
                import org.junit.jupiter.api.BeforeAll;
                import org.junit.jupiter.api.DisplayName;
                import org.junit.jupiter.api.Test;

                import java.io.IOException;
                import java.io.InputStream;
                import java.util.Properties;

                import static io.restassured.RestAssured.given;
                %s

                public class %s {

                    @BeforeAll
                    static void setup() {
                        Properties props = new Properties();
                        try (InputStream is = %s.class.getClassLoader().getResourceAsStream("specforge.properties")) {
                            if (is != null) props.load(is);
                        } catch (IOException ignored) {}

                        String baseUrl = props.getProperty("baseUrl", "http://localhost:8080");
                        RestAssured.baseURI = baseUrl;
                    }

                %s
                }
                """.formatted(basePackage, schemaImport, className, className, indent(methods.toString(), 4));
    }

    private String renderTestMethod(OperationModel op, TestCaseModel tc, String responseSchemaResource) {
        String safeName = toSafeJavaIdentifier(tc.getName());
        String resolvedPath = resolvePathForHappyPath(op);
        RequestContext requestContext = renderRequestSpec(op);
        String llmMethodBody = generateMethodBodyWithLlm(
                tc.getName(),
                op.getHttpMethod(),
                resolvedPath,
                requestContext.payloadJson(),
                tc.getExpectedStatus(),
                responseSchemaResource
        );
        String methodBody = llmMethodBody == null || llmMethodBody.isBlank()
                ? renderFallbackMethodBody(op, resolvedPath, tc.getExpectedStatus(), responseSchemaResource)
                : llmMethodBody;

        return """
                @Test
                @DisplayName("%s %s - %s")
                void %s() {
                    RequestSpecification requestSpec = given()
                %s
                    ;
                %s
                }
                """.formatted(
                op.getHttpMethod(),
                op.getPath(),
                tc.getType(),
                safeName,
                indent(requestContext.requestSpecCode(), 8),
                indent(methodBody, 8)
        );
    }

    private String resolvePathForHappyPath(OperationModel op) {
        String originalPath = op.getPath() == null ? "/" : op.getPath();
        Map<String, String> valuesByParam = new HashMap<>();

        if (op.getParams() != null) {
            for (ParamModel param : op.getParams()) {
                if (param == null || param.getIn() != ParamLocation.PATH || !param.isRequired()) {
                    continue;
                }
                valuesByParam.put(param.getName(), deterministicValueForType(param.getType()));
            }
        }

        Pattern pattern = Pattern.compile("\\{([^}/]+)}");
        Matcher matcher = pattern.matcher(originalPath);
        StringBuffer resolved = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String replacement = valuesByParam.getOrDefault(paramName, "1");
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(resolved);
        return resolved.toString();
    }

    private String deterministicValueForType(String type) {
        if (type == null) {
            return "1";
        }

        String normalized = type.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "boolean" -> "true";
            case "integer", "number", "int32", "int64", "float", "double" -> "1";
            default -> "1";
        };
    }

    private RequestContext renderRequestSpec(OperationModel op) {
        StringBuilder sb = new StringBuilder();
        String payloadJson = "{}";
        sb.append(".accept(ContentType.JSON)\n");

        if (op.getParams() != null) {
            for (ParamModel param : op.getParams()) {
                if (param == null || param.getIn() != ParamLocation.QUERY || !param.isRequired()) {
                    continue;
                }
                sb.append(".queryParam(\"")
                        .append(escapeJavaString(param.getName()))
                        .append("\", ")
                        .append(queryLiteralForType(param.getType()))
                        .append(")\n");
            }
        }

        if (op.getRequestBody() != null && op.getRequestBody().getSchema() != null) {
            String contentType = op.getRequestBody().getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/json";
            }

            Object payload = payloadGenerator.generate(op.getRequestBody().getSchema());
            String jsonPayload = toJson(payload);
            payloadJson = jsonPayload;
            sb.append(".contentType(\"")
                    .append(escapeJavaString(contentType))
                    .append("\")\n")
                    .append(".body(\"")
                    .append(escapeJavaString(jsonPayload))
                    .append("\")\n");
        }

        return new RequestContext(sb.toString().trim(), payloadJson);
    }

    private String renderFallbackMethodBody(OperationModel op,
                                            String resolvedPath,
                                            int expectedStatus,
                                            String responseSchemaResource) {
        String responseSchemaAssertion = responseSchemaResource == null
                ? ""
                : "\n            .body(matchesJsonSchemaInClasspath(\"" + responseSchemaResource + "\"))";

        return """
                requestSpec
                    .when()
                        .request("%s", "%s")
                    .then()
                        .statusCode(%d)%s;
                """.formatted(op.getHttpMethod(), resolvedPath, expectedStatus, responseSchemaAssertion).trim();
    }

    private String generateMethodBodyWithLlm(String scenarioName,
                                             String method,
                                             String url,
                                             String payloadJson,
                                             int expectedStatus,
                                             String responseSchemaResource) {
        if (llmProvider == null) {
            return null;
        }

        String prompt = """
                Generate a RestAssured test method body for a scenario: '%s'.
                Endpoint: %s %s.
                Input JSON: %s.
                Expected Status: %d.
                Use strict assertions for the response body.
                Assume 'requestSpec' is available. Return only the code inside the method.
                """.formatted(
                safe(scenarioName),
                safe(method),
                safe(url),
                safe(payloadJson),
                expectedStatus
        );

        if (responseSchemaResource != null && !responseSchemaResource.isBlank()) {
            prompt = prompt + "\nSchema assertion helper available: matchesJsonSchemaInClasspath(\""
                    + responseSchemaResource + "\").";
        }

        String finalPrompt = prompt;
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> llmProvider.generate(finalPrompt));
        try {
            String generated = future.get(LLM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return sanitizeGeneratedCode(generated);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | TimeoutException e) {
            return null;
        } finally {
            future.cancel(true);
        }
    }

    private String sanitizeGeneratedCode(String generated) {
        if (generated == null) {
            return null;
        }

        String trimmed = generated.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            if (firstLineEnd > 0) {
                trimmed = trimmed.substring(firstLineEnd + 1);
            }
            int fenceEnd = trimmed.lastIndexOf("```");
            if (fenceEnd >= 0) {
                trimmed = trimmed.substring(0, fenceEnd).trim();
            }
        }
        return trimmed;
    }

    private String queryLiteralForType(String type) {
        String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "boolean" -> "true";
            case "integer", "number", "int32", "int64", "float", "double" -> "1";
            default -> "\"value\"";
        };
    }

    private Map<String, String> writeResponseSchemas(List<OperationModel> ops, Path testResRoot) throws IOException {
        Map<String, String> resourceByOperationId = new HashMap<>();
        Path schemasDir = testResRoot.resolve("schemas");
        Files.createDirectories(schemasDir);

        for (OperationModel op : ops) {
            if (op.getPreferredResponse() == null || op.getPreferredResponse().getSchema() == null) {
                continue;
            }

            String fileName = sanitizeFileName(op.getOperationId()) + "_" + op.getPreferredResponse().getStatusCode() + ".json";
            Path schemaPath = schemasDir.resolve(fileName);
            Files.writeString(
                    schemaPath,
                    toJson(op.getPreferredResponse().getSchema()),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
            resourceByOperationId.put(op.getOperationId(), "schemas/" + fileName);
        }

        return resourceByOperationId;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize generated payload/schema", e);
        }
    }

    private String sanitizeFileName(String raw) {
        if (raw == null || raw.isBlank()) {
            return "operation";
        }
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String escapeJavaString(String raw) {
        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String toPascalCase(String s) {
        String[] parts = s.replaceAll("[^a-zA-Z0-9]+", " ").trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1));
        }
        return sb.length() == 0 ? "Default" : sb.toString();
    }

    private String toSafeJavaIdentifier(String s) {
        String cleaned = s.replaceAll("[^a-zA-Z0-9_]", "_");
        if (cleaned.isEmpty()) cleaned = "test";
        if (Character.isDigit(cleaned.charAt(0))) cleaned = "_" + cleaned;
        return cleaned;
    }

    private String indent(String text, int spaces) {
        String pad = " ".repeat(spaces);
        String[] lines = text.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.isBlank()) {
                sb.append("\n");
            } else {
                sb.append(pad).append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private record RequestContext(String requestSpecCode, String payloadJson) {
    }
}
