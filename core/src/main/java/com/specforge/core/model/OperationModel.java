package com.specforge.core.model;

import java.util.ArrayList;
import java.util.List;

public class OperationModel {

    private String operationId;
    private String httpMethod;
    private String path;
    private int preferredSuccessStatus = 200;

    private List<String> tags = new ArrayList<>();
    private List<ParamModel> params = new ArrayList<>();
    private List<TestCaseModel> testCases = new ArrayList<>();

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public int getPreferredSuccessStatus() { return preferredSuccessStatus; }
    public void setPreferredSuccessStatus(int preferredSuccessStatus) { this.preferredSuccessStatus = preferredSuccessStatus; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<ParamModel> getParams() { return params; }
    public void setParams(List<ParamModel> params) { this.params = params; }

    public List<TestCaseModel> getTestCases() { return testCases; }
    public void setTestCases(List<TestCaseModel> testCases) { this.testCases = testCases; }
}
