package com.eppo.sdk.dto;

import java.util.List;

import lombok.Data;

/**
 * Rule Class
 */
@Data
public class Rule {
    private String allocationKey;
    private List<Condition> conditions;
}
