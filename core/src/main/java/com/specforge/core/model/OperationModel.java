package com.specforge.core.model;

import java.util.ArrayList;
import java.util.List;

public class OperationModel {

    private String operationId;
    private String httpMethod;
    private String path;
    private String description;
    private int preferredSuccessStatus = 200;
    private RequestBodyModel requestBody;
    private ResponseModel preferredResponse;

    private List<String> tags = new ArrayList<>();
    private List<String> businessRules = new ArrayList<>();
    private List<ParamModel> params = new ArrayList<>();
    private List<TestCaseModel> testCases = new ArrayList<>();

    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getHttpMethod() { return httpMethod; }
    public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPreferredSuccessStatus() { return preferredSuccessStatus; }
    public void setPreferredSuccessStatus(int preferredSuccessStatus) { this.preferredSuccessStatus = preferredSuccessStatus; }

    public RequestBodyModel getRequestBody() { return requestBody; }
    public void setRequestBody(RequestBodyModel requestBody) { this.requestBody = requestBody; }

    public ResponseModel getPreferredResponse() { return preferredResponse; }
    public void setPreferredResponse(ResponseModel preferredResponse) { this.preferredResponse = preferredResponse; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getBusinessRules() { return businessRules; }
    public void setBusinessRules(List<String> businessRules) { this.businessRules = businessRules; }

    public List<ParamModel> getParams() { return params; }
    public void setParams(List<ParamModel> params) { this.params = params; }

    public List<TestCaseModel> getTestCases() { return testCases; }
    public void setTestCases(List<TestCaseModel> testCases) { this.testCases = testCases; }
}
