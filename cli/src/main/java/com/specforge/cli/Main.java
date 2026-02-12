package com.specforge.cli;

import com.specforge.core.llm.LlmProvider;
import com.specforge.core.llm.LlmProviderFactory;
import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        LlmProvider llmProvider = LlmProviderFactory.createFromConfig();
        int exitCode = new CommandLine(new SpecForgeCommand(llmProvider)).execute(args);
        System.exit(exitCode);
    }
}
