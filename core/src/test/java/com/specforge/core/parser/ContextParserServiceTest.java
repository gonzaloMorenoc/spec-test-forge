package com.specforge.core.parser;

import com.specforge.core.model.ContextModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContextParserServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesJsonRulesByEndpointPath() throws IOException {
        Path file = tempDir.resolve("requirements.json");
        Files.writeString(file, """
                {
                  "rulesByEndpointPath": {
                    "/users": ["age must be greater than 18", "email is required"]
                  }
                }
                """);

        ContextModel model = new ContextParserService().parse(file);

        assertEquals(List.of("age must be greater than 18", "email is required"), model.getRulesForPath("/users"));
    }

    @Test
    void parsesMarkdownHeaderAndBullets() throws IOException {
        Path file = tempDir.resolve("requirements.md");
        Files.writeString(file, """
                ## /users
                - El usuario debe ser mayor de 18
                - El email es obligatorio
                """);

        ContextModel model = new ContextParserService().parse(file);

        assertEquals(List.of("El usuario debe ser mayor de 18", "El email es obligatorio"), model.getRulesForPath("/users"));
    }
}
