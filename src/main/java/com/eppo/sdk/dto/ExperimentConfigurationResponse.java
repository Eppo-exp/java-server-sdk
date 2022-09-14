package com.eppo.sdk.dto;

import java.util.Map;

import lombok.Data;

/**
 * Experiment Configuration Response Class
 */
@Data
public class ExperimentConfigurationResponse {
    private Map<String, ExperimentConfiguration> flags;
}
