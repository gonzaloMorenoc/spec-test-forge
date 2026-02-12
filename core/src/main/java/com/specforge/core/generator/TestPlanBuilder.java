package com.specforge.core.generator;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.TestCaseModel;
import com.specforge.core.model.TestType;

public class TestPlanBuilder {

    public ApiSpecModel build(ApiSpecModel parsed) {
        // Phase 1: Create a single happy-path test per operation (skeleton).
        for (OperationModel op : parsed.getOperations()) {
            TestCaseModel tc = new TestCaseModel();
            tc.setType(TestType.HAPPY_PATH);
            tc.setName(op.getOperationId() + "_happyPath");
            tc.setExpectedStatus(200); // placeholder; next step we infer from OpenAPI responses deterministically
            op.getTestCases().add(tc);
        }
        return parsed;
    }
}