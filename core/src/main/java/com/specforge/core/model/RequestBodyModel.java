package com.specforge.core.model;

import java.util.Map;

public class RequestBodyModel {

    private String contentType;
    private Map<String, Object> schema;

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Map<String, Object> getSchema() { return schema; }
    public void setSchema(Map<String, Object> schema) { this.schema = schema; }
}
