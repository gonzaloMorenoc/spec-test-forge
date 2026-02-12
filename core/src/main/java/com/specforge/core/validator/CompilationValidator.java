package com.specforge.core.validator;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompilationValidator {

    public ValidationResult validate(Path generatedProjectDir) {
        Path testJavaRoot = generatedProjectDir.resolve("src/test/java");
        if (!Files.exists(testJavaRoot)) {
            return new ValidationResult(true, List.of());
        }

        List<Path> generatedSources = listJavaFiles(testJavaRoot);
        if (generatedSources.isEmpty()) {
            return new ValidationResult(true, List.of());
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return new ValidationResult(false, List.of(new ValidationError(
                    null,
                    0,
                    "No JavaCompiler available. Run with a JDK (not JRE)."
            )));
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("specforge-compile-validator-");
            List<Path> stubSources = writeStubSources(tempDir.resolve("stubs"));
            List<File> compilationUnits = new ArrayList<>();
            generatedSources.forEach(path -> compilationUnits.add(path.toFile()));
            stubSources.forEach(path -> compilationUnits.add(path.toFile()));

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            Path classesOut = tempDir.resolve("classes");
            Files.createDirectories(classesOut);

            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
                Iterable<? extends JavaFileObject> javaFiles = fileManager.getJavaFileObjectsFromFiles(compilationUnits);
                List<String> options = List.of("-d", classesOut.toString(), "-Xlint:none");
                Boolean compilationOk = compiler.getTask(null, fileManager, diagnostics, options, null, javaFiles).call();

                List<ValidationError> errors = toValidationErrors(diagnostics.getDiagnostics(), generatedSources);
                boolean success = Boolean.TRUE.equals(compilationOk) && errors.isEmpty();
                return new ValidationResult(success, errors);
            }
        } catch (IOException e) {
            return new ValidationResult(false, List.of(new ValidationError(null, 0, "Validation error: " + e.getMessage())));
        } finally {
            if (tempDir != null) {
                deleteDirectoryQuietly(tempDir);
            }
        }
    }

    private List<Path> listJavaFiles(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private List<ValidationError> toValidationErrors(List<Diagnostic<? extends JavaFileObject>> diagnostics,
                                                     List<Path> generatedSources) {
        Set<Path> generatedSourceSet = generatedSources.stream()
                .map(path -> path.toAbsolutePath().normalize())
                .collect(Collectors.toSet());

        List<ValidationError> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }

            Path sourcePath = null;
            if (diagnostic.getSource() != null && diagnostic.getSource().toUri() != null
                    && Objects.equals("file", diagnostic.getSource().toUri().getScheme())) {
                sourcePath = Path.of(diagnostic.getSource().toUri()).toAbsolutePath().normalize();
            }

            if (sourcePath != null && !generatedSourceSet.contains(sourcePath)) {
                continue;
            }

            long line = diagnostic.getLineNumber() > 0 ? diagnostic.getLineNumber() : 0;
            String message = diagnostic.getMessage(null);
            errors.add(new ValidationError(sourcePath, line, message));
        }

        return errors;
    }

    private List<Path> writeStubSources(Path stubsRoot) throws IOException {
        List<StubSource> stubs = List.of(
                new StubSource("io/restassured/RestAssured.java", """
                        package io.restassured;

                        import io.restassured.response.Response;
                        import io.restassured.specification.RequestSpecification;

                        public final class RestAssured {
                            public static String baseURI;

                            private RestAssured() {
                            }

                            public static RequestSpecification given() {
                                return new RequestSpecification();
                            }
                        }
                        """),
                new StubSource("io/restassured/http/ContentType.java", """
                        package io.restassured.http;

                        public enum ContentType {
                            JSON
                        }
                        """),
                new StubSource("io/restassured/specification/RequestSpecification.java", """
                        package io.restassured.specification;

                        import io.restassured.http.ContentType;
                        import io.restassured.response.Response;

                        public class RequestSpecification {
                            public RequestSpecification accept(ContentType contentType) { return this; }
                            public RequestSpecification queryParam(String name, Object value) { return this; }
                            public RequestSpecification formParam(String name, Object value) { return this; }
                            public RequestSpecification multiPart(String controlName, Object object) { return this; }
                            public RequestSpecification multiPart(String controlName, String fileName, byte[] bytes, String mimeType) { return this; }
                            public RequestSpecification contentType(String contentType) { return this; }
                            public RequestSpecification body(String body) { return this; }
                            public RequestSpecification when() { return this; }
                            public Response request(String method, String path) { return new Response(); }
                        }
                        """),
                new StubSource("io/restassured/response/Response.java", """
                        package io.restassured.response;

                        public class Response {
                            public ValidatableResponse then() { return new ValidatableResponse(); }
                        }
                        """),
                new StubSource("io/restassured/response/ValidatableResponse.java", """
                        package io.restassured.response;

                        public class ValidatableResponse {
                            public ValidatableResponse statusCode(int statusCode) { return this; }
                            public ValidatableResponse body(Object bodyMatcher) { return this; }
                            public ValidatableResponse body(String path, Object bodyMatcher) { return this; }
                        }
                        """),
                new StubSource("io/restassured/module/jsv/JsonSchemaValidator.java", """
                        package io.restassured.module.jsv;

                        public final class JsonSchemaValidator {
                            private JsonSchemaValidator() {
                            }

                            public static Object matchesJsonSchemaInClasspath(String path) {
                                return new Object();
                            }
                        }
                        """),
                new StubSource("org/junit/jupiter/api/Test.java", """
                        package org.junit.jupiter.api;

                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;

                        @Retention(RetentionPolicy.RUNTIME)
                        @Target({ElementType.METHOD})
                        public @interface Test {
                        }
                        """),
                new StubSource("org/junit/jupiter/api/BeforeAll.java", """
                        package org.junit.jupiter.api;

                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;

                        @Retention(RetentionPolicy.RUNTIME)
                        @Target({ElementType.METHOD})
                        public @interface BeforeAll {
                        }
                        """),
                new StubSource("org/junit/jupiter/api/DisplayName.java", """
                        package org.junit.jupiter.api;

                        import java.lang.annotation.ElementType;
                        import java.lang.annotation.Retention;
                        import java.lang.annotation.RetentionPolicy;
                        import java.lang.annotation.Target;

                        @Retention(RetentionPolicy.RUNTIME)
                        @Target({ElementType.TYPE, ElementType.METHOD})
                        public @interface DisplayName {
                            String value();
                        }
                        """),
                new StubSource("org/hamcrest/Matchers.java", """
                        package org.hamcrest;

                        public final class Matchers {
                            private Matchers() {
                            }

                            public static Object greaterThan(int value) {
                                return new Object();
                            }

                            public static Object greaterThanOrEqualTo(int value) {
                                return new Object();
                            }

                            public static Object equalTo(Object value) {
                                return new Object();
                            }
                        }
                        """)
        );

        List<Path> written = new ArrayList<>();
        for (StubSource stub : stubs) {
            Path target = stubsRoot.resolve(stub.relativePath());
            Files.createDirectories(target.getParent());
            Files.writeString(target, stub.content(), StandardCharsets.UTF_8);
            written.add(target);
        }
        return written;
    }

    private void deleteDirectoryQuietly(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best effort cleanup.
                }
            });
        } catch (IOException ignored) {
            // Best effort cleanup.
        }
    }

    public record ValidationError(Path file, long line, String message) {
    }

    public record ValidationResult(boolean success, List<ValidationError> errors) {
        public String formatForPrompt() {
            if (errors == null || errors.isEmpty()) {
                return "Unknown compilation error.";
            }

            StringBuilder sb = new StringBuilder();
            for (ValidationError error : errors) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                String fileName = error.file() == null ? "unknown" : error.file().toString();
                sb.append(fileName)
                        .append(":")
                        .append(error.line())
                        .append(" -> ")
                        .append(error.message());
            }
            return sb.toString();
        }

        public Map<Path, List<ValidationError>> errorsByFile() {
            if (errors == null || errors.isEmpty()) {
                return Map.of();
            }
            return errors.stream()
                    .filter(error -> error.file() != null)
                    .collect(Collectors.groupingBy(ValidationError::file));
        }
    }

    private record StubSource(String relativePath, String content) {
    }
}
