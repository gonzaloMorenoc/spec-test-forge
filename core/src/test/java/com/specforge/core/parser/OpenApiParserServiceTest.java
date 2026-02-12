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
}
