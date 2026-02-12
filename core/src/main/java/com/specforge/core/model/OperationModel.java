package com.specforge.core.model;

import java.util.ArrayList;
import java.util.List;

public class OperationModel {

    private String operationId;
    private String httpMethod;
    private String path;

    private List<String> tags = new ArrayList<>();
    private List<TestCaseModel> testCases = new ArrayList<>();

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<TestCaseModel> getTestCases() { return testCases; }
    public void setTestCases(List<TestCaseModel> testCases) { this.testCases = testCases; }
}