package com.specforge.cli;

import picocli.CommandLine;

public class Main {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new SpecForgeCommand()).execute(args);
        System.exit(exitCode);
    }
}