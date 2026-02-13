package com.specforge.cli;

import com.specforge.core.exporter.GenerationMode;
import com.specforge.core.exporter.RestAssuredProjectExporter;
import com.specforge.core.generator.TestPlanBuilder;
import com.specforge.core.llm.LlmProvider;
import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.ContextModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.parser.ContextParserService;
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

    private final LlmProvider llmProvider;

    public SpecForgeCommand(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    @Option(names = {"--spec"}, required = true, description = "Path to OpenAPI spec (yaml/json).")
    private String specPath;

    @Option(names = {"--output"}, required = true, description = "Output directory.")
    private String outputDir;

    @Option(names = {"--basePackage"}, defaultValue = "com.generated.api", description = "Base Java package for generated tests.")
    private String basePackage;

    @Option(names = {"--mode"}, defaultValue = "new-project", description = "Generation mode: new-project | tests-only")
    private String mode;

    @Option(names = {"--baseUrl"}, defaultValue = "http://localhost:8080", description = "Base URL for RestAssured.baseURI")
    private String baseUrl;

    @Option(names = {"--context"}, required = false, description = "Path to business context file (.md/.json).")
    private String contextPath;

    @Override
    public void run() {
        GenerationMode generationMode = parseMode(mode);

        Path spec = resolveSpecPath(specPath);
        Path out = resolveOutputPath(outputDir);
        ContextModel contextModel = resolveContext(contextPath);

        OpenApiParserService parser = new OpenApiParserService();
        ApiSpecModel parsed = parser.parse(spec.toString()); // now absolute path
        applyBusinessContext(parsed, contextModel);

        TestPlanBuilder builder = new TestPlanBuilder(new AiScenarioPlanner(llmProvider, contextModel));
        ApiSpecModel plan = builder.build(parsed);

        RestAssuredProjectExporter exporter = new RestAssuredProjectExporter(llmProvider);
        exporter.export(plan, out, basePackage, generationMode, baseUrl);

        System.out.println("Generated tests successfully.");
        System.out.println("Mode: " + generationMode);
        System.out.println("Output: " + out.toAbsolutePath());
        System.out.println("Operations: " + plan.getOperations().size());
    }

    private ContextModel resolveContext(String rawContextPath) {
        if (rawContextPath == null || rawContextPath.isBlank()) {
            return new ContextModel();
        }

        Path contextFile = resolveSpecPath(rawContextPath);
        ContextParserService contextParser = new ContextParserService();
        return contextParser.parse(contextFile);
    }

    private void applyBusinessContext(ApiSpecModel parsed, ContextModel contextModel) {
        if (parsed == null || parsed.getOperations() == null || contextModel == null) {
            return;
        }

        for (OperationModel operation : parsed.getOperations()) {
            if (operation == null) {
                continue;
            }
            operation.setBusinessRules(new ArrayList<>(contextModel.getRulesForPath(operation.getPath())));
        }
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
            case "new-project", "standalone" -> GenerationMode.STANDALONE;
            case "tests-only", "embedded" -> GenerationMode.EMBEDDED;
            default -> throw new IllegalArgumentException("Invalid --mode. Use: new-project | tests-only");
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
