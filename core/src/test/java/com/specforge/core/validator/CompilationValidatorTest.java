package com.specforge.core.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompilationValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void validatesGeneratedSourcesWhenCodeIsCompilable() throws IOException {
        Path sourceRoot = tempDir.resolve("src/test/java/com/generated/api");
        Files.createDirectories(sourceRoot);

        Files.writeString(sourceRoot.resolve("UsersApiTest.java"), """
                package com.generated.api;

                import io.restassured.specification.RequestSpecification;

                import static io.restassured.RestAssured.given;

                class UsersApiTest {
                    void test() {
                        RequestSpecification requestSpec = given();
                        requestSpec.when().request("GET", "/users/1").then().statusCode(200);
                    }
                }
                """, StandardCharsets.UTF_8);

        CompilationValidator.ValidationResult result = new CompilationValidator().validate(tempDir);

        assertTrue(result.success());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void reportsCompilationErrorsWhenCodeIsInvalid() throws IOException {
        Path sourceRoot = tempDir.resolve("src/test/java/com/generated/api");
        Files.createDirectories(sourceRoot);

        Files.writeString(sourceRoot.resolve("UsersApiTest.java"), """
                package com.generated.api;

                class UsersApiTest {
                    void test() {
                        int x = 1
                    }
                }
                """, StandardCharsets.UTF_8);

        CompilationValidator.ValidationResult result = new CompilationValidator().validate(tempDir);

        assertFalse(result.success());
        assertFalse(result.errors().isEmpty());
        assertTrue(result.formatForPrompt().contains("UsersApiTest.java"));
    }
}
