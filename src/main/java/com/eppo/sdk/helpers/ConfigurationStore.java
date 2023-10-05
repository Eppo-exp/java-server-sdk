package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.ExperimentConfiguration;
import com.eppo.sdk.dto.ExperimentConfigurationResponse;
import com.eppo.sdk.exception.ExperimentConfigurationNotFound;
import com.eppo.sdk.exception.NetworkException;
import com.eppo.sdk.exception.NetworkRequestNotAllowed;
import org.ehcache.Cache;

import java.util.Map;
import java.util.Optional;

/**
 * Configuration Store Class
 */
public class ConfigurationStore {
    Cache<String, ExperimentConfiguration> experimentConfigurationCache;
    ExperimentConfigurationRequestor experimentConfigurationRequestor;
    static ConfigurationStore instance = null;

    public ConfigurationStore(
            Cache<String, ExperimentConfiguration> experimentConfigurationCache,
            ExperimentConfigurationRequestor experimentConfigurationRequestor) {
        this.experimentConfigurationRequestor = experimentConfigurationRequestor;
        this.experimentConfigurationCache = experimentConfigurationCache;
    }

    /**
     * This function is used to initialize configuration store
     *
     * @param experimentConfigurationCache
     * @param experimentConfigurationRequestor
     * @return
     */
    public final static ConfigurationStore init(
            Cache<String, ExperimentConfiguration> experimentConfigurationCache,
            ExperimentConfigurationRequestor experimentConfigurationRequestor) {
        if (ConfigurationStore.instance == null) {
            ConfigurationStore.instance = new ConfigurationStore(
                    experimentConfigurationCache,
                    experimentConfigurationRequestor);
        }
        instance.experimentConfigurationCache.clear();
        return ConfigurationStore.instance;
    }

    /**
     * This function is used to get initialized instance
     *
     * @return
     */
    public final static ConfigurationStore getInstance() {
        return ConfigurationStore.instance;
    }

    /**
     * This function is used to set experiment configuration to cache
     *
     * @param key
     * @param experimentConfiguration
     */
    public void setExperimentConfiguration(String key, ExperimentConfiguration experimentConfiguration) {
        this.experimentConfigurationCache.put(key, experimentConfiguration);
    }

    /**
     * This function is used to fetch experiment configuration
     *
     * @param key
     * @return
     * @throws ExperimentConfigurationNotFound
     */
    public ExperimentConfiguration getExperimentConfiguration(String key)
            throws ExperimentConfigurationNotFound {
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
        Optional<ExperimentConfigurationResponse> response = this.experimentConfigurationRequestor
                .fetchExperimentConfiguration();

        if (!response.isEmpty()) {
            for (Map.Entry<String, ExperimentConfiguration> entry : response.get().getFlags().entrySet()) {
                this.setExperimentConfiguration(entry.getKey(), entry.getValue());
            }
        }
    }
}
