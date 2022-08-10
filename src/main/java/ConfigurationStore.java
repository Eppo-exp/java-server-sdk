import dto.ExperimentConfiguration;
import dto.ExperimentConfigurationResponse;
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

    final static ConfigurationStore getInstance(
            Cache<String, ExperimentConfiguration> experimentConfigurationCache,
            ExperimentConfigurationRequestor experimentConfigurationRequestor
    ) {
        if (ConfigurationStore.instance == null) {
            ConfigurationStore.instance = new ConfigurationStore(experimentConfigurationCache, experimentConfigurationRequestor);
        }

        return ConfigurationStore.instance;
    }

    private void setExperimentConfiguration(String key, ExperimentConfiguration experimentConfiguration) {
        this.experimentConfigurationCache.put(key, experimentConfiguration);
    }

    public ExperimentConfiguration getExperimentConfiguration(String key) throws NetworkException, NetworkRequestNotAllowed {
        if (!this.experimentConfigurationCache.containsKey(key)) {
            this.fetchAndSetExperimentConfiguration();
        }
        return this.experimentConfigurationCache.get(key);
    }

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

    public boolean isFetchingExperimentConfigurationAllowed() {
        return this.experimentConfigurationRequestor.isRequestAllowed();
    }
}
