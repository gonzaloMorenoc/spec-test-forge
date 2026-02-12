package com.specforge.core.model;

public class TestCaseModel {

    private TestType type;
    private String name;
    private int expectedStatus;

    public TestType getType() { return type; }
    public void setType(TestType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getExpectedStatus() { return expectedStatus; }
    public void setExpectedStatus(int expectedStatus) { this.expectedStatus = expectedStatus; }
}