package com.specforge.core.model;

import java.util.Map;

public class ResponseModel {

    private int statusCode;
    private String contentType;
    private Map<String, Object> schema;

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Map<String, Object> getSchema() { return schema; }
    public void setSchema(Map<String, Object> schema) { this.schema = schema; }
}
