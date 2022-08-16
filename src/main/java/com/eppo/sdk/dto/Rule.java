package com.eppo.sdk.dto;

import java.util.List;

/**
 * Rule Class
 */
public class Rule {
    public List<Condition> conditions;

    @Override
    public String toString() {
        return "[Conditions: " + conditions + "]";
    }
}
