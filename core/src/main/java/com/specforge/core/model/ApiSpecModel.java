package com.specforge.core.model;

import java.util.ArrayList;
import java.util.List;

public class ApiSpecModel {

    private String title;
    private String version;
    private List<OperationModel> operations = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public List<OperationModel> getOperations() { return operations; }
    public void setOperations(List<OperationModel> operations) { this.operations = operations; }
}