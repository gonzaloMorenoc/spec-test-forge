package com.specforge.core.exporter;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.ParamLocation;
import com.specforge.core.model.ParamModel;
import com.specforge.core.model.TestCaseModel;
import com.specforge.core.model.TestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RestAssuredProjectExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void rendersResolvedPathForRequiredPathParams() throws IOException {
        ParamModel id = new ParamModel();
        id.setName("id");
        id.setIn(ParamLocation.PATH);
        id.setRequired(true);
        id.setType("integer");

        TestCaseModel testCase = new TestCaseModel();
        testCase.setType(TestType.HAPPY_PATH);
        testCase.setName("getUser_happyPath");
        testCase.setExpectedStatus(200);

        OperationModel operation = new OperationModel();
        operation.setOperationId("getUser");
        operation.setHttpMethod("GET");
        operation.setPath("/users/{id}");
        operation.setTags(List.of("users"));
        operation.setParams(List.of(id));
        operation.setTestCases(List.of(testCase));

        ApiSpecModel model = new ApiSpecModel();
        model.setOperations(List.of(operation));

        new RestAssuredProjectExporter().export(
                model,
                tempDir,
                "com.generated.api",
                GenerationMode.EMBEDDED,
                "http://localhost:8080"
        );

        Path javaFile = tempDir.resolve("src/test/java/com/generated/api/UsersApiTest.java");
        String generated = Files.readString(javaFile);

        assertTrue(generated.contains(".request(\"GET\", \"/users/1\")"));
        assertTrue(generated.contains(".statusCode(200);"));
    }
}
