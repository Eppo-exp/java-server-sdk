package com.eppo.sdk.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.ExperimentConfigurationResponse;
import com.eppo.sdk.exception.NetworkException;

import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * Experiment Configuration Requestor Class
 */
public class ExperimentConfigurationRequestor {
    private EppoHttpClient eppoHttpClient;
    private boolean isRequestAllowedVar = true;

    public ExperimentConfigurationRequestor(EppoHttpClient eppoHttpClient) {
        this.eppoHttpClient = eppoHttpClient;
    }

    /**
     * This function is used to fetch Experiment Configuration
     *
     * @return
     * @throws NetworkException
     */
    public Optional<ExperimentConfigurationResponse> fetchExperimentConfiguration() throws NetworkException {
        ExperimentConfigurationResponse config = null;
        try {
            HttpResponse<String> response = this.eppoHttpClient.get(Constants.RAC_ENDPOINT);
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                config = objectMapper.readValue(response.body(), ExperimentConfigurationResponse.class);
            }

            // Set if next request is allowed or not
            this.setIsRequestAllowed(statusCode);
        } catch (Exception e) {
            throw new NetworkException("Unable to Fetch Experiment Configuration");
        }

        return Optional.ofNullable(config);
    }

    /**
     * This function is used to set isRequestAllowedVar parameter
     *
     * @param statusCode
     */
    private void setIsRequestAllowed(int statusCode) {
        if (statusCode >= 400 && statusCode < 500) {
            // 429 - Too many request 408 - Request timeout
            this.isRequestAllowedVar = statusCode == 429 || statusCode == 408;
        } else {
            this.isRequestAllowedVar = true;
        }
    }

    /***
     * This function is used to fetch iSRequestAllowedVar
     * @return
     */
    public boolean isRequestAllowed() {
        return this.isRequestAllowedVar;
    }
}