package com.eppo.sdk;

import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.*;
import com.eppo.sdk.helpers.*;
import com.eppo.sdk.exception.*;
import org.ehcache.Cache;

import java.util.List;
import java.util.Optional;

public class EppoClient {
    /**
     * Static Instance
     */
    private static EppoClient instance = null;
    private ConfigurationStore configurationStore;

    private Poller poller;

    private EppoClientConfig eppoClientConfig;

    private EppoClient(ConfigurationStore configurationStore, Poller poller, EppoClientConfig eppoClientConfig) {
        this.configurationStore = configurationStore;
        this.poller = poller;
        this.eppoClientConfig = eppoClientConfig;
        poller.run();
    }

    /**
     * This function is used to get assignment Value
     *
     * @param subjectKey
     * @param experimentKey
     * @param subjectAttributes
     * @return
     * @throws Exception
     */
    public Optional<String> getAssignment(
            String subjectKey,
            String experimentKey,
            SubjectAttributes subjectAttributes
    ) throws Exception {
        // Validate Input Values
        InputValidator.validateNotBlank(subjectKey, "Invalid argument: subjectKey cannot be blank");
        InputValidator.validateNotBlank(experimentKey, "Invalid argument: experimentKey cannot be blank");

        // Fetch Experiment Configuration
        ExperimentConfiguration configuration = this.configurationStore.getExperimentConfiguration(experimentKey);

        // Check if subject has override variations
        String subjectVariationOverride = this.getSubjectVariationOverride(subjectKey, configuration);
        if (subjectVariationOverride != null) {
            return Optional.of(subjectVariationOverride);
        }

        // If disabled or not in Experiment Sampler or Rules not satisfied return empty string
        if (!configuration.enabled ||
                !this.isInExperimentSample(subjectKey, experimentKey, configuration) ||
                !this.subjectAttributesSatisfyRules(subjectAttributes, configuration.rules)
        ) {
            return Optional.empty();
        }

        // Get assigned variation
        Variation assignedVariation = this.getAssignedVariation(subjectKey, experimentKey, configuration);

        try {
            if (this.eppoClientConfig.getAssignmentLogger().isPresent()) {
                this.eppoClientConfig.getAssignmentLogger()
                        .get()
                        .logAssignment(new AssignmentLogData(
                                experimentKey,
                                assignedVariation.name,
                                subjectKey,
                                subjectAttributes
                        ));
            }
        } catch (Exception e){
            // Ignore Exception
        }
        return Optional.of(assignedVariation.name);
    }

    /**
     * This function is used to get assignment without passing subject attribute
     *
     * @param subjectKey
     * @param experimentKey
     * @return
     * @throws Exception
     */
    public Optional<String> getAssignment(String subjectKey, String experimentKey) throws Exception {
        return this.getAssignment(subjectKey, experimentKey, new SubjectAttributes());
    }

    /**
     * This function is used to check if the Experiment is in the same
     *
     * @param subjectKey
     * @param experimentKey
     * @param experimentConfiguration
     * @return
     * @throws Exception
     */
    private boolean isInExperimentSample(
            String subjectKey,
            String experimentKey,
            ExperimentConfiguration experimentConfiguration
    ) throws Exception {
        int subjectShards = experimentConfiguration.subjectShards;
        float percentageExposure = experimentConfiguration.percentExposure;

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
     * @throws Exception
     */
    private Variation getAssignedVariation(
            String subjectKey,
            String experimentKey,
            ExperimentConfiguration experimentConfiguration
    ) throws Exception {
        int subjectShards = experimentConfiguration.subjectShards;
        int shard = Shard.getShard("assignment-" + subjectKey + "-" + experimentKey, subjectShards);

        Optional<Variation> variation = experimentConfiguration.variations.stream()
                .filter(config -> Shard.isShardInRange(shard, config.shardRange))
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
    private String getSubjectVariationOverride(
            String subjectKey,
            ExperimentConfiguration experimentConfiguration
    ) throws Exception {
        String hexedSubjectKey = Shard.getHex(subjectKey);
        return experimentConfiguration.overrides.getOrDefault(hexedSubjectKey, null);
    }

    /**
     * This function is used to test if subject attributes are satisfying rules or not
     *
     * @param subjectAttributes
     * @param rules
     * @return
     * @throws Exception
     */
    private boolean subjectAttributesSatisfyRules(
            SubjectAttributes subjectAttributes,
            List<Rule> rules
    ) throws Exception {
        if (rules.size() == 0) {
            return true;
        }
        return RuleValidator.matchesAnyRule(subjectAttributes, rules);
    }

    /***
     * This function is used to initialize the Eppo Client
     * @param eppoClientConfig
     * @return
     */
    public static synchronized EppoClient init(EppoClientConfig eppoClientConfig) {
        // Create eppo http client
        // @to-do: read sdkName and sdkVersion from pom.xml file
        EppoHttpClient eppoHttpClient = new EppoHttpClient(
                eppoClientConfig.getApiKey(),
                "java-server-sdk",
                "1.0.0",
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

        // Create Poller
        Poller poller = Poller.init(
                () -> {
                    try {
                        if (configurationStore.isFetchingExperimentConfigurationAllowed()) {
                            configurationStore.fetchAndSetExperimentConfiguration();
                            return true;
                        }
                    } catch (Exception e) {
                    }
                    return false;
                },
                Constants.TIME_INTERVAL_IN_MILLIS,
                Constants.JITTER_INTERVAL_IN_MILLIS
        );

        // Stop polling if the Eppo client is already initialized before
        if (EppoClient.instance != null) {
            EppoClient.instance.poller.stop();
        }

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
