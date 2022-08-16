package com.eppo.sdk.dto;

import java.util.List;
import java.util.Map;

/**
 * Experiment Configuration Class
 */
public class ExperimentConfiguration {
    public String name;
    public boolean enabled;
    public int subjectShards;
    public float percentExposure;
    public List<Variation> variations;
    public Map<String, String> overrides;
    public List<Rule> rules;

    @Override
    public String toString() {
        return "[Name: " + name + " | Enabled: " + enabled + " | SubjectShards: " +
                subjectShards + " | PercentExposure: " + percentExposure + " | Variations: " +
                variations.toString() + " | Overrides: " + overrides.toString() + " | Rules: " + rules.toString() +  "]";
    }
}
