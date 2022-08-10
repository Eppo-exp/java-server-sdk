package helpers;

import dto.ExperimentConfiguration;
import dto.ExperimentConfigurationResponse;
import exception.ExperimentConfigurationNotFound;
import exception.NetworkException;
import exception.NetworkRequestNotAllowed;
import org.ehcache.Cache;

import java.util.Map;
import java.util.Optional;

public class ConfigurationStore {
    Cache<String, ExperimentConfiguration> experimentConfigurationCache;
    ExperimentConfigurationRequestor experimentConfigurationRequestor;
    static ConfigurationStore instance = null;

    public ConfigurationStore(
            Cache<String, ExperimentConfiguration> experimentConfigurationCache,
            ExperimentConfigurationRequestor experimentConfigurationRequestor
    ) {
        this.experimentConfigurationRequestor = experimentConfigurationRequestor;
        this.experimentConfigurationCache = experimentConfigurationCache;
    }

    public final static ConfigurationStore init(
            Cache<String, ExperimentConfiguration> experimentConfigurationCache,
            ExperimentConfigurationRequestor experimentConfigurationRequestor
    ) {
        if (ConfigurationStore.instance == null) {
            ConfigurationStore.instance = new ConfigurationStore(experimentConfigurationCache, experimentConfigurationRequestor);
        }

        return ConfigurationStore.instance;
    }

    public final static ConfigurationStore getInstance() {
        return ConfigurationStore.instance;
    }

    /**
     * This function is used to set experiment configuration to cache
     *
     * @param key
     * @param experimentConfiguration
     */
    private void setExperimentConfiguration(String key, ExperimentConfiguration experimentConfiguration) {
        this.experimentConfigurationCache.put(key, experimentConfiguration);
    }

    /**
     * This function is used to fetch experiment configuration
     *
     * @param key
     * @return
     * @throws NetworkException
     * @throws NetworkRequestNotAllowed
     * @throws ExperimentConfigurationNotFound
     */
    public ExperimentConfiguration getExperimentConfiguration(String key) throws NetworkException, NetworkRequestNotAllowed, ExperimentConfigurationNotFound {
        if (!this.experimentConfigurationCache.containsKey(key)) {
            this.fetchAndSetExperimentConfiguration();
        }
        try {
            return this.experimentConfigurationCache.get(key);
        } catch (Exception e) {
            throw new ExperimentConfigurationNotFound("Experiment configuration not found!");
        }

    }

    /**
     * This function is used to set experiment configuration int the cache
     *
     * @throws NetworkException
     * @throws NetworkRequestNotAllowed
     */
    public void fetchAndSetExperimentConfiguration() throws NetworkException, NetworkRequestNotAllowed {
        System.out.println(this.isFetchingExperimentConfigurationAllowed());
        if (!this.isFetchingExperimentConfigurationAllowed()) {
            throw new NetworkRequestNotAllowed("Fetching Experiment Configuration is not allowed");
        }
        Optional<ExperimentConfigurationResponse> response = this.experimentConfigurationRequestor.fetchExperimentConfiguration();
        if (!response.isEmpty()) {
            for (Map.Entry<String, ExperimentConfiguration> entry : response.get().experiments.entrySet()) {
                this.setExperimentConfiguration(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * This function is used to check if it is allowed to fetch experiment configuration from network
     *
     * @return
     */
    public boolean isFetchingExperimentConfigurationAllowed() {
        return this.experimentConfigurationRequestor.isRequestAllowed();
    }
}
