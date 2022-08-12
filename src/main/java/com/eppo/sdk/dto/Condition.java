package com.eppo.sdk.dto;

/**
 * Rule's Condition Class
 */
public class Condition {
    public OperatorType operator;
    public String attribute;
    public EppoValue value;

    @Override
    public String toString() {
        return "[Operator: " + operator + " | Attribute: " + attribute + " | Value: " + value.toString() + "]";
    }
}
