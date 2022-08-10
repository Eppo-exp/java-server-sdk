import com.fasterxml.jackson.databind.ObjectMapper;
import dto.ExperimentConfigurationResponse;
import exception.NetworkException;
import helpers.EppoHttpClient;

import java.net.http.HttpResponse;
import java.util.Optional;

public class ExperimentConfigurationRequestor {
    private EppoHttpClient eppoHttpClient;
    private boolean isRequestAllowedVar =  true;

    public ExperimentConfigurationRequestor(
            EppoHttpClient eppoHttpClient
    ){
        this.eppoHttpClient = eppoHttpClient;
    }

    /**
     * This function is used to fetch Experiment Configuration
     * @return
     * @throws NetworkException
     */
    public Optional<ExperimentConfigurationResponse> fetchExperimentConfiguration() throws NetworkException {
        ExperimentConfigurationResponse config = null;
        try {
            HttpResponse<String> response  = this.eppoHttpClient.get(Constants.RAC_ENDPOINT);
            System.out.println(response);
            int statusCode = response.statusCode();
            if (statusCode == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                config = objectMapper
                        .readValue(response.body(), ExperimentConfigurationResponse.class);
            }
            this.setIsRequestAllowed(statusCode);
        } catch (Exception e) {
            throw new NetworkException("Unable to Fetch Experiment Configuration");
        }

        return Optional.ofNullable(config);
    }

    /**
     * This function is used to set isRequestAllowedVar paramter
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