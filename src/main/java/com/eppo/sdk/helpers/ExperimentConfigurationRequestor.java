package com.eppo.sdk.helpers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.ExperimentConfigurationResponse;
import com.eppo.sdk.exception.InvalidApiKeyException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.util.Optional;

/**
 * Experiment Configuration Requestor Class
 */
@Slf4j
public class ExperimentConfigurationRequestor {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
            HttpResponse response = this.eppoHttpClient.get(Constants.RAC_ENDPOINT);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                config = OBJECT_MAPPER.readValue(EntityUtils.toString(response.getEntity()), ExperimentConfigurationResponse.class);
            }
            if (statusCode == 401) { // unauthorized - invalid API key
                throw new InvalidApiKeyException("Unauthorized: invalid Eppo API key.");
            }
        } catch (InvalidApiKeyException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unable to Fetch Experiment Configuration: " + e.getMessage());
        }

        return Optional.ofNullable(config);
    }
}