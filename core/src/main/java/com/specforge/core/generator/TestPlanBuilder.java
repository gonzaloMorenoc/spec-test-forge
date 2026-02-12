package com.specforge.core.generator;

import com.specforge.core.model.ApiSpecModel;
import com.specforge.core.model.OperationModel;
import com.specforge.core.model.TestCaseModel;
import com.specforge.core.model.TestType;
import com.specforge.core.planner.AiScenarioPlanner;
import com.specforge.core.planner.TestScenario;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TestPlanBuilder {

    private final AiScenarioPlanner scenarioPlanner;

    public TestPlanBuilder() {
        this(null);
    }

    public TestPlanBuilder(AiScenarioPlanner scenarioPlanner) {
        this.scenarioPlanner = scenarioPlanner;
    }

    public ApiSpecModel build(ApiSpecModel parsed) {
        for (OperationModel op : parsed.getOperations()) {
            List<TestScenario> scenarios = scenarioPlanner != null ? scenarioPlanner.plan(op) : List.of();
            List<TestCaseModel> testCases = new ArrayList<>();

            if (scenarios.isEmpty()) {
                testCases.add(defaultHappyPath(op));
            } else {
                int index = 1;
                for (TestScenario scenario : scenarios) {
                    if (scenario == null) {
                        continue;
                    }

                    TestCaseModel tc = new TestCaseModel();
                    tc.setType(inferType(scenario));
                    tc.setName(resolveScenarioName(op, scenario, index));
                    tc.setExpectedStatus(scenario.getExpectedStatus() > 0
                            ? scenario.getExpectedStatus()
                            : op.getPreferredSuccessStatus());
                    testCases.add(tc);
                    index++;
                }

                if (testCases.isEmpty()) {
                    testCases.add(defaultHappyPath(op));
                }
            }

            op.setTestCases(testCases);
        }
        return parsed;
    }

    private TestCaseModel defaultHappyPath(OperationModel op) {
        TestCaseModel tc = new TestCaseModel();
        tc.setType(TestType.HAPPY_PATH);
        tc.setName(op.getOperationId() + "_happyPath");
        tc.setExpectedStatus(resolveHappyPathStatus(op));
        return tc;
    }

    private int resolveHappyPathStatus(OperationModel op) {
        int status = op.getPreferredSuccessStatus();
        if (status >= 200 && status < 300) {
            return status;
        }
        return 200;
    }

    private String resolveScenarioName(OperationModel op, TestScenario scenario, int index) {
        String value = scenario.getName();
        if (value == null || value.isBlank()) {
            return op.getOperationId() + "_scenario_" + index;
        }
        return value;
    }

    private TestType inferType(TestScenario scenario) {
        String text = ((scenario.getName() == null ? "" : scenario.getName()) + " "
                + (scenario.getDescription() == null ? "" : scenario.getDescription()))
                .toLowerCase(Locale.ROOT);

        if (text.contains("security") || text.contains("auth") || text.contains("unauthorized")
                || text.contains("forbidden") || text.contains("injection")) {
            return TestType.SECURITY;
        }
        if (text.contains("boundary") || text.contains("limit") || text.contains("max")
                || text.contains("min") || text.contains("edge")) {
            return TestType.BOUNDARY;
        }
        if (text.contains("negative") || text.contains("invalid") || text.contains("error")
                || text.contains("bad request")) {
            return TestType.NEGATIVE;
        }
        return TestType.HAPPY_PATH;
    }
}
