package com.specforge.core.planner;

import com.specforge.core.model.ContextModel;
import com.specforge.core.model.OperationModel;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiScenarioPlannerTest {

    @Test
    void parsesJsonScenariosFromLlmResponse() {
        OperationModel operation = new OperationModel();
        operation.setHttpMethod("POST");
        operation.setPath("/users");
        operation.setDescription("Create user");

        String response = """
                [
                  {"name":"Happy path create","description":"Valid body","expectedStatus":201},
                  {"name":"Boundary long name","description":"Name at max length","expectedStatus":400}
                ]
                """;

        AiScenarioPlanner planner = new AiScenarioPlanner(prompt -> response, Duration.ofSeconds(1));
        List<TestScenario> scenarios = planner.plan(operation);

        assertEquals(2, scenarios.size());
        assertEquals("Happy path create", scenarios.getFirst().getName());
        assertEquals(201, scenarios.getFirst().getExpectedStatus());
    }

    @Test
    void returnsEmptyWhenLlmResponseIsInvalidJson() {
        OperationModel operation = new OperationModel();
        operation.setHttpMethod("GET");
        operation.setPath("/users/{id}");

        AiScenarioPlanner planner = new AiScenarioPlanner(prompt -> "not-json", Duration.ofSeconds(1));
        List<TestScenario> scenarios = planner.plan(operation);

        assertTrue(scenarios.isEmpty());
    }

    @Test
    void includesBusinessRulesFromContextInPrompt() {
        OperationModel operation = new OperationModel();
        operation.setHttpMethod("POST");
        operation.setPath("/users");
        operation.setDescription("Create user");

        ContextModel contextModel = new ContextModel();
        contextModel.addRule("/users", "El usuario debe ser mayor de 18");

        AtomicReference<String> capturedPrompt = new AtomicReference<>();
        AiScenarioPlanner planner = new AiScenarioPlanner(prompt -> {
            capturedPrompt.set(prompt);
            return "[]";
        }, null, contextModel, Duration.ofSeconds(1));

        planner.plan(operation);

        String prompt = capturedPrompt.get();
        assertTrue(prompt.contains("Ademas del Swagger"));
        assertTrue(prompt.contains("El usuario debe ser mayor de 18"));
    }
}
