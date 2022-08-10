package dto;

import java.util.List;

public class Rule {
    public List<Condition> conditions;

    @Override
    public String toString() {
        return "[Conditions: " + conditions + "]";
    }
}
