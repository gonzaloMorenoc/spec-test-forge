package com.specforge.core.parser;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.ParamLocation;
import com.specforge.core.model.ParamModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenApiParserServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void infersPreferredSuccessStatusUsingPriorityOrderAndFallback() throws IOException {
        Path specFile = tempDir.resolve("status-spec.yaml");
        Files.writeString(specFile, """
                openapi: 3.0.0
                info:
                  title: Status API
                  version: "1.0.0"
                paths:
                  /items:
                    get:
                      operationId: getItems
                      responses:
                        "200":
                          description: ok
                        "201":
                          description: created
                        "204":
                          description: no content
                  /legacy:
                    get:
                      operationId: getLegacy
                      responses:
                        "404":
                          description: not found
                        "400":
                          description: bad request
                """);

        ApiSpecModel model = new OpenApiParserService().parse(specFile.toString());
        Map<String, OperationModel> byId = model.getOperations().stream()
                .collect(Collectors.toMap(OperationModel::getOperationId, Function.identity()));

        assertEquals(201, byId.get("getItems").getPreferredSuccessStatus());
        assertEquals(400, byId.get("getLegacy").getPreferredSuccessStatus());
    }

    @Test
    void extractsPathQueryAndHeaderParameters() throws IOException {
        Path specFile = tempDir.resolve("params-spec.yaml");
        Files.writeString(specFile, """
                openapi: 3.0.0
                info:
                  title: Params API
                  version: "1.0.0"
                paths:
                  /users/{id}:
                    parameters:
                      - name: id
                        in: path
                        required: true
                        schema:
                          type: integer
                    get:
                      operationId: getUser
                      parameters:
                        - name: includeDeleted
                          in: query
                          required: true
                          schema:
                            type: boolean
                        - name: X-Trace-Id
                          in: header
                          required: false
                          schema:
                            type: string
                      responses:
                        "200":
                          description: ok
                """);

        ApiSpecModel model = new OpenApiParserService().parse(specFile.toString());
        OperationModel op = model.getOperations().stream()
                .filter(it -> "getUser".equals(it.getOperationId()))
                .findFirst()
                .orElseThrow();

        Map<String, ParamModel> paramsByName = op.getParams().stream()
                .collect(Collectors.toMap(ParamModel::getName, Function.identity()));

        ParamModel id = paramsByName.get("id");
        assertNotNull(id);
        assertEquals(ParamLocation.PATH, id.getIn());
        assertEquals(true, id.isRequired());
        assertEquals("integer", id.getType());

        ParamModel includeDeleted = paramsByName.get("includeDeleted");
        assertNotNull(includeDeleted);
        assertEquals(ParamLocation.QUERY, includeDeleted.getIn());
        assertEquals(true, includeDeleted.isRequired());
        assertEquals("boolean", includeDeleted.getType());

        ParamModel traceId = paramsByName.get("X-Trace-Id");
        assertNotNull(traceId);
        assertEquals(ParamLocation.HEADER, traceId.getIn());
        assertEquals(false, traceId.isRequired());
        assertEquals("string", traceId.getType());
    }

    @Test
    void extractsRequestBodyAndPreferredResponseSchemaResolvingRefs() throws IOException {
        Path specFile = tempDir.resolve("body-and-schema-spec.yaml");
        Files.writeString(specFile, """
                openapi: 3.0.0
                info:
                  title: Body API
                  version: "1.0.0"
                paths:
                  /users:
                    post:
                      operationId: createUser
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              $ref: "#/components/schemas/UserCreateRequest"
                      responses:
                        "201":
                          description: created
                          content:
                            application/json:
                              schema:
                                $ref: "#/components/schemas/UserResponse"
                components:
                  schemas:
                    UserCreateRequest:
                      type: object
                      required: [name]
                      properties:
                        name:
                          type: string
                          minLength: 3
                        age:
                          type: integer
                          minimum: 18
                    UserResponse:
                      type: object
                      required: [id, name]
                      properties:
                        id:
                          type: integer
                        name:
                          type: string
                """);

        ApiSpecModel model = new OpenApiParserService().parse(specFile.toString());
        OperationModel op = model.getOperations().stream()
                .filter(it -> "createUser".equals(it.getOperationId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(op.getRequestBody());
        assertEquals("application/json", op.getRequestBody().getContentType());
        Map<String, Object> requestSchema = op.getRequestBody().getSchema();
        assertEquals("object", requestSchema.get("type"));
        assertTrue(((Map<?, ?>) requestSchema.get("properties")).containsKey("name"));

        assertNotNull(op.getPreferredResponse());
        assertEquals(201, op.getPreferredResponse().getStatusCode());
        Map<String, Object> responseSchema = op.getPreferredResponse().getSchema();
        assertEquals("object", responseSchema.get("type"));
        assertTrue(((Map<?, ?>) responseSchema.get("properties")).containsKey("id"));
    }

    @Test
    void parsesSwagger2SpecByConvertingToOpenApiModel() throws IOException {
        Path specFile = tempDir.resolve("swagger2-spec.json");
        Files.writeString(specFile, """
                {
                  "swagger": "2.0",
                  "info": {
                    "title": "Swagger2 API",
                    "version": "1.0.0"
                  },
                  "paths": {
                    "/pets/{id}": {
                      "get": {
                        "operationId": "getPet",
                        "parameters": [
                          {
                            "name": "id",
                            "in": "path",
                            "required": true,
                            "type": "integer"
                          },
                          {
                            "name": "verbose",
                            "in": "query",
                            "required": true,
                            "type": "boolean"
                          }
                        ],
                        "responses": {
                          "200": {
                            "description": "ok"
                          }
                        }
                      }
                    }
                  }
                }
                """);

        ApiSpecModel model = new OpenApiParserService().parse(specFile.toString());
        OperationModel op = model.getOperations().stream()
                .filter(it -> "getPet".equals(it.getOperationId()))
                .findFirst()
                .orElseThrow();

        assertEquals("GET", op.getHttpMethod());
        assertEquals("/pets/{id}", op.getPath());
        assertEquals(200, op.getPreferredSuccessStatus());
        assertEquals(2, op.getParams().size());
    }
}
