package com.specforge.core.exporter;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.ParamLocation;
import com.specforge.core.model.ParamModel;
import com.specforge.core.model.TestCaseModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestAssuredProjectExporter {

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
            Path testResRoot  = outputDir.resolve("src/test/resources");
            Files.createDirectories(testJavaRoot);
            Files.createDirectories(testResRoot);

            writeBaseTestConfig(testResRoot, baseUrl);

            Map<String, List<OperationModel>> byTag = groupByPrimaryTag(model.getOperations());
            for (Map.Entry<String, List<OperationModel>> entry : byTag.entrySet()) {
                String tag = entry.getKey();
                String className = toPascalCase(tag) + "ApiTest";
                String java = renderTestClass(basePackage, className, entry.getValue());

                Path pkgDir = testJavaRoot.resolve(basePackage.replace('.', '/'));
                Files.createDirectories(pkgDir);

                Files.writeString(pkgDir.resolve(className + ".java"), java, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to export tests: " + e.getMessage(), e);
        }
    }

    private void writeStandaloneGradleProject(Path outputDir) throws IOException {
        Files.writeString(outputDir.resolve("settings.gradle"),
                "rootProject.name = \"generated-api-tests\"\n",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
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
                }

                test {
                    useJUnitPlatform()
                }
                """;

        Files.writeString(outputDir.resolve("build.gradle"),
                buildGradle,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void writeBaseTestConfig(Path testResRoot, String baseUrl) throws IOException {
        String content = "baseUrl=" + (baseUrl == null ? "http://localhost:8080" : baseUrl) + "\n";
        Files.writeString(testResRoot.resolve("specforge.properties"),
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
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

    private String renderTestClass(String basePackage, String className, List<OperationModel> ops) {
        StringBuilder methods = new StringBuilder();

        for (OperationModel op : ops) {
            for (TestCaseModel tc : op.getTestCases()) {
                methods.append(renderTestMethod(op, tc)).append("\n");
            }
        }

        return """
                package %s;

                import io.restassured.RestAssured;
                import io.restassured.http.ContentType;
                import org.junit.jupiter.api.BeforeAll;
                import org.junit.jupiter.api.DisplayName;
                import org.junit.jupiter.api.Test;

                import java.io.IOException;
                import java.io.InputStream;
                import java.util.Properties;

                import static io.restassured.RestAssured.given;

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
                """.formatted(basePackage, className, className, indent(methods.toString(), 4));
    }

    private String renderTestMethod(OperationModel op, TestCaseModel tc) {
        String safeName = toSafeJavaIdentifier(tc.getName());
        String resolvedPath = resolvePathForHappyPath(op);

        return """
                @Test
                @DisplayName("%s %s - %s")
                void %s() {
                    given()
                        .accept(ContentType.JSON)
                    .when()
                        .request("%s", "%s")
                    .then()
                        .statusCode(%d);
                }
                """.formatted(op.getHttpMethod(), op.getPath(), tc.getType(), safeName, op.getHttpMethod(), resolvedPath, tc.getExpectedStatus());
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
}
