package com.specforge.cli;

import com.specforge.core.exporter.GenerationMode;
import com.specforge.core.exporter.RestAssuredProjectExporter;
import com.specforge.core.generator.TestPlanBuilder;
import com.specforge.core.llm.LlmProvider;
import com.specforge.core.llm.OllamaLlmProvider;
import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.parser.OpenApiParserService;
import com.specforge.core.planner.AiScenarioPlanner;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

        LlmProvider llmProvider = new OllamaLlmProvider();
        TestPlanBuilder builder = new TestPlanBuilder(new AiScenarioPlanner(llmProvider));
        ApiSpecModel plan = builder.build(parsed);

        RestAssuredProjectExporter exporter = new RestAssuredProjectExporter(llmProvider);
        exporter.export(plan, out, basePackage, generationMode, baseUrl);

        System.out.println("Generated tests successfully.");
        System.out.println("Mode: " + generationMode);
        System.out.println("Output: " + out.toAbsolutePath());
        System.out.println("Operations: " + plan.getOperations().size());
    }

    private Path resolveSpecPath(String raw) {
        Path input = Path.of(raw);
        List<Path> tried = new ArrayList<>();

        if (input.isAbsolute()) {
            Path absolute = input.normalize();
            tried.add(absolute);
            if (Files.exists(absolute)) {
                return absolute;
            }
        } else {
            Path cwd = currentWorkingDir();
            Path fromCwd = cwd.resolve(input).normalize();
            tried.add(fromCwd);
            if (Files.exists(fromCwd)) {
                return fromCwd;
            }

            Path repoRoot = repoRootOrCwd(cwd);
            Path fromRepoRoot = repoRoot.resolve(input).normalize();
            if (!fromRepoRoot.equals(fromCwd)) {
                tried.add(fromRepoRoot);
            }
            if (Files.exists(fromRepoRoot)) {
                return fromRepoRoot;
            }
        }

        throw new IllegalArgumentException("Spec file not found: " + raw + "\nTried:\n - " + joinTriedPaths(tried));
    }

    private Path resolveOutputPath(String raw) {
        Path input = Path.of(raw);
        if (input.isAbsolute()) {
            return input.normalize();
        }

        Path cwd = currentWorkingDir();
        Path repoRoot = repoRootOrCwd(cwd);
        return repoRoot.resolve(input).normalize();
    }

    private GenerationMode parseMode(String raw) {
        String v = raw == null ? "" : raw.trim().toLowerCase();
        return switch (v) {
            case "standalone" -> GenerationMode.STANDALONE;
            case "embedded" -> GenerationMode.EMBEDDED;
            default -> throw new IllegalArgumentException("Invalid --mode. Use: standalone | embedded");
        };
    }

    private Path currentWorkingDir() {
        return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private Path repoRootOrCwd(Path cwd) {
        if (cwd.getFileName() != null && "cli".equals(cwd.getFileName().toString())) {
            Path parent = cwd.getParent();
            if (parent != null && Files.exists(parent.resolve("settings.gradle.kts"))) {
                return parent.normalize();
            }
        }
        return cwd;
    }

    private String joinTriedPaths(List<Path> tried) {
        if (tried.isEmpty()) {
            return "(none)";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tried.size(); i++) {
            if (i > 0) {
                sb.append("\n - ");
            }
            sb.append(tried.get(i));
        }
        return sb.toString();
    }
}
