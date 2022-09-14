package com.eppo.sdk;

import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.Allocation;
import com.eppo.sdk.dto.AssignmentLogData;
import com.eppo.sdk.dto.EppoClientConfig;
import com.eppo.sdk.dto.EppoValue;
import com.eppo.sdk.dto.ExperimentConfiguration;
import com.eppo.sdk.dto.Rule;
import com.eppo.sdk.dto.SubjectAttributes;
import com.eppo.sdk.dto.Variation;
import com.eppo.sdk.exception.EppoClientIsNotInitializedException;
import com.eppo.sdk.exception.InvalidInputException;
import com.eppo.sdk.helpers.AppDetails;
import com.eppo.sdk.helpers.CacheHelper;
import com.eppo.sdk.helpers.ConfigurationStore;
import com.eppo.sdk.helpers.EppoHttpClient;
import com.eppo.sdk.helpers.ExperimentConfigurationRequestor;
import com.eppo.sdk.helpers.FetchConfigurationsTask;
import com.eppo.sdk.helpers.InputValidator;
import com.eppo.sdk.helpers.RuleValidator;
import com.eppo.sdk.helpers.Shard;

import lombok.extern.slf4j.Slf4j;

import org.ehcache.Cache;

import java.util.List;
import java.util.Optional;
import java.util.Timer;

@Slf4j
public class EppoClient {
    /**
     * Static Instance
     */
    private static EppoClient instance = null;
    private ConfigurationStore configurationStore;

    private Timer poller;

    private EppoClientConfig eppoClientConfig;

    private EppoClient(ConfigurationStore configurationStore, Timer poller, EppoClientConfig eppoClientConfig) {
        this.configurationStore = configurationStore;
        this.poller = poller;
        this.eppoClientConfig = eppoClientConfig;
    }

    /**
     * This function is used to get assignment Value
     *
     * @param subjectKey
     * @param flagKey
     * @param subjectAttributes
     * @return
     */
    public Optional<String> getAssignment(
            String subjectKey,
            String flagKey,
            SubjectAttributes subjectAttributes
    ) {
        // Validate Input Values
        InputValidator.validateNotBlank(subjectKey, "Invalid argument: subjectKey cannot be blank");
        InputValidator.validateNotBlank(flagKey, "Invalid argument: flagKey cannot be blank");

        // Fetch Experiment Configuration
        ExperimentConfiguration configuration = this.configurationStore.getExperimentConfiguration(flagKey);
        if (configuration == null) {
            log.warn("[Eppo SDK] No configuration found for key: " + flagKey);
            return Optional.empty();
        }

        // Check if subject has override variations
        EppoValue subjectVariationOverride = this.getSubjectVariationOverride(subjectKey, configuration);
        if (!subjectVariationOverride.isNull()) {
            return Optional.of(subjectVariationOverride.stringValue());
        }

        // Check if disabled
        if (!configuration.isEnabled()) {
            log.info("[Eppo SDK] No assigned variation because the experiment or feature flag {} is disabled", flagKey);
            return Optional.empty();
        }

        // Find matched rule
        Optional<Rule> rule = RuleValidator.findMatchingRule(subjectAttributes, configuration.getRules());
        if (rule.isEmpty()) {
            log.info("[Eppo SDK] No assigned variation. The subject attributes did not match any targeting rules");
            return Optional.empty();
        }

        // Check if in experiment sample
        Allocation allocation = configuration.getAllocation(rule.get().getAllocationKey());
        if (!this.isInExperimentSample(subjectKey, flagKey, configuration.getSubjectShards(), allocation.getPercentExposure())) {
            log.info("[Eppo SDK] No assigned variation. The subject is not part of the sample population");
            return Optional.empty();
        }

        // Get assigned variation
        Variation assignedVariation = this.getAssignedVariation(subjectKey, flagKey, configuration.getSubjectShards(), allocation.getVariations());

        try {
            this.eppoClientConfig.getAssignmentLogger()
                .logAssignment(new AssignmentLogData(
                        flagKey,
                        assignedVariation.getValue().stringValue(),
                        subjectKey,
                        subjectAttributes
                ));
        } catch (Exception e){
            // Ignore Exception
        }
        return Optional.of(assignedVariation.getValue().stringValue());
    }

    /**
     * This function is used to get assignment without passing subject attribute
     *
     * @param subjectKey
     * @param experimentKey
     * @return
     */
    public Optional<String> getAssignment(String subjectKey, String experimentKey) {
        return this.getAssignment(subjectKey, experimentKey, new SubjectAttributes());
    }

    /**
     * This function is used to check if the Experiment is in the same
     *
     * @param subjectKey
     * @param experimentKey
     * @param experimentConfiguration
     * @return
     */
    private boolean isInExperimentSample(
            String subjectKey,
            String experimentKey,
            int subjectShards,
            float percentageExposure
    ) {
        int shard = Shard.getShard("exposure-" + subjectKey + "-" + experimentKey, subjectShards);
        return shard <= percentageExposure * subjectShards;
    }

    /**
     * This function is used to get assigned variation
     *
     * @param subjectKey
     * @param experimentKey
     * @param experimentConfiguration
     * @return
     */
    private Variation getAssignedVariation(
            String subjectKey,
            String experimentKey,
            int subjectShards,
            List<Variation> variations
    ) {
        int shard = Shard.getShard("assignment-" + subjectKey + "-" + experimentKey, subjectShards);

        Optional<Variation> variation = variations.stream()
                .filter(config -> Shard.isShardInRange(shard, config.getShardRange()))
                .findFirst();

        return variation.get();
    }

    /**
     * This function is used to get override variations.
     *
     * @param subjectKey
     * @param experimentConfiguration
     * @return
     */
    private EppoValue getSubjectVariationOverride(
            String subjectKey,
            ExperimentConfiguration experimentConfiguration
    ) {
        String hexedSubjectKey = Shard.getHex(subjectKey);
        return experimentConfiguration.getOverrides().getOrDefault(hexedSubjectKey, new EppoValue());
    }

    /***
     * This function is used to initialize the Eppo Client
     * @param eppoClientConfig
     * @return
     */
    public static synchronized EppoClient init(EppoClientConfig eppoClientConfig) {
        InputValidator.validateNotBlank(eppoClientConfig.getApiKey(), "An API key is required");
        if (eppoClientConfig.getAssignmentLogger() == null) {
            throw new InvalidInputException("An assignment logging implementation is required");
        }
        // Create eppo http client
        AppDetails appDetails = AppDetails.getInstance();
        EppoHttpClient eppoHttpClient = new EppoHttpClient(
                eppoClientConfig.getApiKey(),
                appDetails.getName(),
                appDetails.getVersion(),
                eppoClientConfig.getBaseURL(),
                Constants.REQUEST_TIMEOUT_MILLIS
        );

        // Create wrapper for fetching experiment configuration
        ExperimentConfigurationRequestor expConfigRequestor = new ExperimentConfigurationRequestor(eppoHttpClient);
        // Create Caching for Experiment Configuration
        CacheHelper cacheHelper = new CacheHelper();
        Cache<String, ExperimentConfiguration> experimentConfigurationCache = cacheHelper
                .createExperimentConfigurationCache(Constants.MAX_CACHE_ENTRIES);
        // Create ExperimentConfiguration Store
        ConfigurationStore configurationStore = ConfigurationStore.init(
                experimentConfigurationCache,
                expConfigRequestor
        );

        // Stop the polling process of any previously initialized client 
        if (EppoClient.instance != null) {
            EppoClient.instance.poller.cancel();
        }

        // Start polling for experiment configurations
        Timer poller = new Timer(true);
        FetchConfigurationsTask fetchConfigurationsTask = new FetchConfigurationsTask(configurationStore, poller, Constants.TIME_INTERVAL_IN_MILLIS, Constants.JITTER_INTERVAL_IN_MILLIS);
        fetchConfigurationsTask.run();

        // Create Eppo Client
        EppoClient eppoClient = new EppoClient(configurationStore, poller, eppoClientConfig);
        EppoClient.instance = eppoClient;

        return eppoClient;
    }

    /**
     * This function is used to get EppoClient instance
     *
     * @return
     * @throws EppoClientIsNotInitializedException
     */
    public static EppoClient getInstance() throws EppoClientIsNotInitializedException {
        if (EppoClient.instance == null) {
            throw new EppoClientIsNotInitializedException("Eppo client is not initialized!");
        }

        return EppoClient.instance;
    }
}
