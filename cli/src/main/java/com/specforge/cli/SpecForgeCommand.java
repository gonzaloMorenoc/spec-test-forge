package com.specforge.cli;

import com.specforge.core.exporter.GenerationMode;
import com.specforge.core.exporter.RestAssuredProjectExporter;
import com.specforge.core.generator.TestPlanBuilder;
import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.parser.OpenApiParserService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.nio.file.Files;
import java.nio.file.Path;

import java.nio.file.Path;

@Command(
        name = "spec-test-forge",
        mixinStandardHelpOptions = true,
        version = "0.1.0",
        description = "Generate REST Assured + JUnit5 API tests from an OpenAPI spec."
)
public class SpecForgeCommand implements Runnable {

    @Option(names = {"--spec"}, required = true, description = "Path to OpenAPI spec (yaml/json).")
    private String specPath;

    @Option(names = {"--output"}, required = true, description = "Output directory.")
    private String outputDir;

    @Option(names = {"--basePackage"}, defaultValue = "com.generated.api", description = "Base Java package for generated tests.")
    private String basePackage;

    @Option(names = {"--mode"}, defaultValue = "standalone", description = "Generation mode: standalone | embedded")
    private String mode;

    @Option(names = {"--baseUrl"}, defaultValue = "http://localhost:8080", description = "Base URL for RestAssured.baseURI")
    private String baseUrl;

    @Override
    public void run() {
        GenerationMode generationMode = parseMode(mode);

        Path spec = resolveSpecPath(specPath);
        Path out = resolveOutputPath(outputDir);

        OpenApiParserService parser = new OpenApiParserService();
        ApiSpecModel parsed = parser.parse(spec.toString()); // now absolute path

        TestPlanBuilder builder = new TestPlanBuilder();
        ApiSpecModel plan = builder.build(parsed);

        RestAssuredProjectExporter exporter = new RestAssuredProjectExporter();
        exporter.export(plan, out, basePackage, generationMode, baseUrl);

        System.out.println("Generated tests successfully.");
        System.out.println("Mode: " + generationMode);
        System.out.println("Output: " + out.toAbsolutePath());
        System.out.println("Operations: " + plan.getOperations().size());
    }

    private Path resolveSpecPath(String raw) {
        Path p = Path.of(raw);
        if (p.isAbsolute() && Files.exists(p)) return p;

        // 1) relative to current working directory (likely cli/)
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path cwdResolved = cwd.resolve(raw).normalize();
        if (Files.exists(cwdResolved)) return cwdResolved;

        // 2) fallback: relative to repo root (parent of cli module)
        Path repoRootGuess = cwd.getParent(); // if running inside .../spec-test-forge/cli
        if (repoRootGuess != null) {
            Path rootResolved = repoRootGuess.resolve(raw).normalize();
            if (Files.exists(rootResolved)) return rootResolved;
        }

        // 3) final: try from current module root variations (common case: examples is at repo root)
        if (repoRootGuess != null) {
            Path alt = repoRootGuess.resolve("examples").resolve(p.getFileName().toString()).normalize();
            if (Files.exists(alt)) return alt;
        }

        throw new IllegalArgumentException("Spec file not found. Tried: " + cwdResolved + " and " +
                (repoRootGuess != null ? repoRootGuess.resolve(raw).normalize() : "(no repo root guess)"));
    }

    private Path resolveOutputPath(String raw) {
        Path p = Path.of(raw);
        if (p.isAbsolute()) return p.normalize();

        // Make output relative to repo root (more intuitive)
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path repoRootGuess = cwd.getParent(); // if running in cli/
        if (repoRootGuess != null) {
            return repoRootGuess.resolve(raw).normalize();
        }
        return cwd.resolve(raw).normalize();
    }

    private GenerationMode parseMode(String raw) {
        String v = raw == null ? "" : raw.trim().toLowerCase();
        return switch (v) {
            case "standalone" -> GenerationMode.STANDALONE;
            case "embedded" -> GenerationMode.EMBEDDED;
            default -> throw new IllegalArgumentException("Invalid --mode. Use: standalone | embedded");
        };
    }
}