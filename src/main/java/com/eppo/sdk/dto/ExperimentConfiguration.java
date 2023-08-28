package com.eppo.sdk.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * Experiment Configuration Class
 */
@Data
public class ExperimentConfiguration {
    private String name;
    private boolean enabled;
    private int subjectShards;
    private Map<String, EppoValue> typedOverrides = new HashMap<>();
    private Map<String, Allocation> allocations;
    private List<Rule> rules;

    public Allocation getAllocation(String allocationKey) {
        return getAllocations().get(allocationKey);
    }
}
