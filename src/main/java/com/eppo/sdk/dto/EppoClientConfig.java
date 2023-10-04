package com.eppo.sdk.dto;

import com.eppo.sdk.constants.Constants;

import lombok.Builder;
import lombok.Data;

/**
 * Eppo Client Config class
 */
@Builder
@Data
public class EppoClientConfig {
    private String apiKey;
    @Builder.Default
    private String baseURL = Constants.DEFAULT_BASE_URL;
    private IAssignmentLogger assignmentLogger;

    /**
     * When set to true, the client will not throw an exception when it encounters
     * an error.
     */
    @Builder.Default
    private boolean isGracefulMode = true;
}
