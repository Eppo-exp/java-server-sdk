package dto;

public class Condition {
    public OperatorType operator;
    public String attribute;
    public String value;

    @Override
    public String toString() {
        return "[Operator: " + operator + " | Attribute: " + attribute + " | Value: " + value + "]";
    }
}
