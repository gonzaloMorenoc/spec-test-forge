package com.specforge.core.model;

public class ParamModel {

    private String name;
    private ParamLocation in;
    private boolean required;
    private String type;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ParamLocation getIn() { return in; }
    public void setIn(ParamLocation in) { this.in = in; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
