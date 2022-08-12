package com.eppo.sdk.dto;

import com.eppo.sdk.constants.Constants;

import java.util.Optional;

/**
 * Eppo Client Config class
 */
public class EppoClientConfig {
    private String apiKey;
    private String baseURL = Constants.DEFAULT_BASE_URL;
    private Optional<IAssignmentLogger> assignmentLogger = Optional.empty();

    public EppoClientConfig(String apiKey) {
        this.apiKey = apiKey;
    }

    public EppoClientConfig(String apiKey, String baseURL) {
        this.apiKey = apiKey;
        this.baseURL = baseURL;
    }

    public EppoClientConfig(String apiKey, IAssignmentLogger assignmentLogger) {
        this.apiKey = apiKey;
        this.assignmentLogger = Optional.of(assignmentLogger);
    }

    public EppoClientConfig(String apiKey, String baseURL, IAssignmentLogger assignmentLogger) {
        this.apiKey = apiKey;
        this.baseURL = baseURL;
        this.assignmentLogger = Optional.of(assignmentLogger);
    }


    public String getApiKey() {
        return apiKey;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public Optional<IAssignmentLogger> getAssignmentLogger() {
        return assignmentLogger;
    }
}
