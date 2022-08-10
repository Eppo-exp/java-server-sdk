package dto;

import java.util.Map;

public class ExperimentConfigurationResponse {
    public Map<String, ExperimentConfiguration> experiments;

    @Override
    public String toString() {
       return "[Experiments: " + experiments.toString() + "]";
    }
}
