package com.specforge.core.generator;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.TestCaseModel;
import org.junit.jupiter.api.Test;

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
}
