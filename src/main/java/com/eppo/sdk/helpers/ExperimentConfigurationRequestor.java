package com.eppo.sdk.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.ExperimentConfigurationResponse;
import com.eppo.sdk.exception.InvalidApiKeyException;
import com.eppo.sdk.exception.NetworkException;

import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.Optional;

/**
 * Experiment Configuration Requestor Class
 */
@Slf4j
public class ExperimentConfigurationRequestor {
    private EppoHttpClient eppoHttpClient;

    public ExperimentConfigurationRequestor(EppoHttpClient eppoHttpClient) {
        this.eppoHttpClient = eppoHttpClient;
    }

    /**
     * This function is used to fetch Experiment Configuration
     *
     * @return
     */
    public Optional<ExperimentConfigurationResponse> fetchExperimentConfiguration() {
        ExperimentConfigurationResponse config = null;
        try {
            HttpResponse<String> response = this.eppoHttpClient.get(Constants.RAC_ENDPOINT);
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                config = objectMapper.readValue(response.body(), ExperimentConfigurationResponse.class);
            }
            if (statusCode == 401) { // unauthorized - invalid API key
                throw new InvalidApiKeyException("Unauthorized: invalid Eppo API key.");
            }
        } catch (HttpTimeoutException e) { // non-fatal error
            log.error("Request time out while fetching experiment configurations: " + e.getMessage(), e);
        } catch (InvalidApiKeyException e) {
            throw e;
        } catch (Exception e) { // fatal error that will stop the polling process
            throw new NetworkException("Unable to Fetch Experiment Configuration: " + e.getMessage());
        }

        return Optional.ofNullable(config);
    }
}
