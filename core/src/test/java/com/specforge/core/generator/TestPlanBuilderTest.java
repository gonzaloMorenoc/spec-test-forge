package com.specforge.core.generator;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.TestCaseModel;
import com.specforge.core.model.TestType;
import com.specforge.core.planner.AiScenarioPlanner;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestPlanBuilderTest {

    @Test
    void usesPreferredSuccessStatusForHappyPathExpectedStatus() {
        OperationModel operation = new OperationModel();
        operation.setOperationId("createUser");
        operation.setHttpMethod("POST");
        operation.setPath("/users");
        operation.setPreferredSuccessStatus(202);

        ApiSpecModel parsed = new ApiSpecModel();
        parsed.setOperations(List.of(operation));

        ApiSpecModel plan = new TestPlanBuilder().build(parsed);
        TestCaseModel tc = plan.getOperations().getFirst().getTestCases().getFirst();

        assertEquals(202, tc.getExpectedStatus());
        assertEquals("createUser_happyPath", tc.getName());
    }

    @Test
    void buildsTestCasesFromAiScenariosWhenAvailable() {
        OperationModel operation = new OperationModel();
        operation.setOperationId("getUser");
        operation.setHttpMethod("GET");
        operation.setPath("/users/{id}");
        operation.setDescription("Returns a user by id");
        operation.setPreferredSuccessStatus(200);

        ApiSpecModel parsed = new ApiSpecModel();
        parsed.setOperations(List.of(operation));

        String response = """
                [
                  {"name":"Security unauthorized access","description":"Call without token","expectedStatus":401},
                  {"name":"Boundary max id","description":"Use max supported id","expectedStatus":200}
                ]
                """;

        AiScenarioPlanner planner = new AiScenarioPlanner(prompt -> response, Duration.ofSeconds(1));
        ApiSpecModel plan = new TestPlanBuilder(planner).build(parsed);
        List<TestCaseModel> testCases = plan.getOperations().getFirst().getTestCases();

        assertEquals(2, testCases.size());
        assertEquals("Security unauthorized access", testCases.get(0).getName());
        assertEquals(401, testCases.get(0).getExpectedStatus());
        assertEquals(TestType.SECURITY, testCases.get(0).getType());
        assertEquals(TestType.BOUNDARY, testCases.get(1).getType());
    }
}
