package com.specforge.core.exporter;

import com.specforge.core.llm.LlmProvider;
import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.ParamLocation;
import com.specforge.core.model.ParamModel;
import com.specforge.core.model.RequestBodyModel;
import com.specforge.core.model.ResponseModel;
import com.specforge.core.model.TestCaseModel;
import com.specforge.core.model.TestType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestAssuredProjectExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void rendersMethodBodyUsingLlmProvider() throws IOException {
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
        operation.setParams(List.of(id, requiredQueryParam("verbose", "boolean")));
        operation.setTestCases(List.of(testCase));
        operation.setRequestBody(sampleRequestBody());
        operation.setPreferredResponse(sampleResponseModel(200));

        ApiSpecModel model = new ApiSpecModel();
        model.setOperations(List.of(operation));

        LlmProvider llmProvider = prompt -> """
                requestSpec
                    .when()
                    .request("GET", "/users/1")
                    .then()
                    .statusCode(200)
                    .body(matchesJsonSchemaInClasspath("schemas/getUser_200.json"));
                """;

        new RestAssuredProjectExporter(llmProvider).export(
                model,
                tempDir,
                "com.generated.api",
                GenerationMode.EMBEDDED,
                "http://localhost:8080"
        );

        Path javaFile = tempDir.resolve("src/test/java/com/generated/api/UsersApiTest.java");
        String generated = Files.readString(javaFile);
        Path schemaFile = tempDir.resolve("src/test/resources/schemas/getUser_200.json");
        String schemaJson = Files.readString(schemaFile);

        assertTrue(generated.contains("RequestSpecification requestSpec = given()"));
        assertTrue(generated.contains(".queryParam(\"verbose\", true)"));
        assertTrue(generated.contains(".body(\"{\\\"name\\\":\\\"value\\\"}\")"));
        assertTrue(generated.contains(".request(\"GET\", \"/users/1\")"));
        assertTrue(generated.contains("matchesJsonSchemaInClasspath(\"schemas/getUser_200.json\")"));
        assertTrue(generated.contains(".statusCode(200)"));
        assertFalse(schemaJson.isBlank());
        assertTrue(schemaJson.contains("\"type\":\"object\""));
    }

    private ParamModel requiredQueryParam(String name, String type) {
        ParamModel param = new ParamModel();
        param.setName(name);
        param.setIn(ParamLocation.QUERY);
        param.setRequired(true);
        param.setType(type);
        return param;
    }

    private RequestBodyModel sampleRequestBody() {
        RequestBodyModel body = new RequestBodyModel();
        body.setContentType("application/json");
        body.setSchema(Map.of(
                "type", "object",
                "required", List.of("name"),
                "properties", Map.of(
                        "name", Map.of("type", "string")
                )
        ));
        return body;
    }

    private ResponseModel sampleResponseModel(int status) {
        ResponseModel response = new ResponseModel();
        response.setStatusCode(status);
        response.setContentType("application/json");
        response.setSchema(Map.of(
                "type", "object",
                "properties", Map.of(
                        "id", Map.of("type", "integer")
                )
        ));
        return response;
    }
}
