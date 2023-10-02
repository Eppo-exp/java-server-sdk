package com.eppo.sdk;

import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.Allocation;
import com.eppo.sdk.dto.AssignmentLogData;
import com.eppo.sdk.dto.EppoClientConfig;
import com.eppo.sdk.dto.EppoValue;
import com.eppo.sdk.dto.EppoValueType;
import com.eppo.sdk.dto.ExperimentConfiguration;
import com.eppo.sdk.dto.Rule;
import com.eppo.sdk.dto.EppoAttributes;
import com.eppo.sdk.dto.Variation;
import com.eppo.sdk.exception.EppoClientIsNotInitializedException;
import com.eppo.sdk.exception.InvalidInputException;
import com.eppo.sdk.helpers.AppDetails;
import com.eppo.sdk.helpers.CacheHelper;
import com.eppo.sdk.helpers.ConfigurationStore;
import com.eppo.sdk.helpers.EppoHttpClient;
import com.eppo.sdk.helpers.ExperimentConfigurationRequestor;
import com.eppo.sdk.helpers.ExperimentHelper;
import com.eppo.sdk.helpers.FetchConfigurationsTask;
import com.eppo.sdk.helpers.InputValidator;
import com.eppo.sdk.helpers.RuleValidator;
import com.eppo.sdk.helpers.Shard;
import com.eppo.sdk.helpers.bandit.BanditEvaluator;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

import org.ehcache.Cache;

import java.util.*;
import java.util.stream.Collectors;

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
    private Optional<EppoValue> getAssignmentValue(
            String subjectKey,
            String flagKey,
            EppoAttributes subjectAttributes,
            Map<String, EppoAttributes> assignmentOptions
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
            return Optional.of(subjectVariationOverride);
        }

        // Check if disabled
        if (!configuration.isEnabled()) {
            log.info("[Eppo SDK] No assigned variation because the experiment or feature flag {} is disabled", flagKey);
            return Optional.empty();
        }

        // Used to assign
        List<Variation> variations;

        // Used for logging
        String assignmentModelVersion = "sharding v1";
        String allocationKey;
        EppoAttributes assignmentAttributes = null;

        if (configuration.isBandit()) {
            allocationKey = "bandit";

            // Properties of the bandit model are hardcoded for now
            String modelName = "random";
            String modelVersion = "0.1";

            assignmentModelVersion = modelName+" "+modelVersion;

            variations = BanditEvaluator.evaluateBanditVariations(
              flagKey,
              modelName,
              assignmentOptions,
              subjectKey,
              subjectAttributes,
              configuration.getSubjectShards()
            );
        } else {
            // Find matched rule
            Optional<Rule> rule = RuleValidator.findMatchingRule(subjectAttributes, configuration.getRules());
            if (rule.isEmpty()) {
                log.info("[Eppo SDK] No assigned variation. The subject attributes did not match any targeting rules");
                return Optional.empty();
            }

            // Check if in experiment sample
            allocationKey = rule.get().getAllocationKey();
            Allocation allocation = configuration.getAllocation(allocationKey);
            if (!this.isInExperimentSample(subjectKey, flagKey, configuration.getSubjectShards(),
              allocation.getPercentExposure())) {
                log.info("[Eppo SDK] No assigned variation. The subject is not part of the sample population");
                return Optional.empty();
            }

            variations = allocation.getVariations();
        }

        // Get assigned variation
        Variation assignedVariation = this.getAssignedVariation(subjectKey, flagKey, configuration.getSubjectShards(), variations);
        String assignedVariationString = assignedVariation.getTypedValue().stringValue();
        float assignedVariationProbability = (float)(assignedVariation.getShardRange().end - assignedVariation.getShardRange().start + 1) / configuration.getSubjectShards();

        if (assignmentOptions != null && !assignmentOptions.isEmpty()) {
            assignmentAttributes = assignmentOptions.get(assignedVariationString);
        }

        try {
            String experimentKey = ExperimentHelper.generateKey(flagKey, allocationKey);
            this.eppoClientConfig.getAssignmentLogger()
                    .logAssignment(new AssignmentLogData(
                            experimentKey,
                            flagKey,
                            assignmentModelVersion,
                            allocationKey,
                            assignedVariationString,
                            assignedVariationProbability,
                            assignmentAttributes,
                            subjectKey,
                            subjectAttributes));
        } catch (Exception e) {
            log.warn("Error logging assignment", e);
        }
        return Optional.of(assignedVariation.getTypedValue());
    }

    /**
     * This function will return typed assignment value
     * 
     * @param subjectKey
     * @param experimentKey
     * @param type
     * @param subjectAttributes
     * @return
     */
    private Optional<?> getTypedAssignment(
            EppoValueType type,
            String subjectKey,
            String experimentKey,
            EppoAttributes subjectAttributes,
            Map<String, EppoAttributes> assignmentOptions
    ) {
        Optional<EppoValue> value = this.getAssignmentValue(subjectKey, experimentKey, subjectAttributes, assignmentOptions);
        if (value.isEmpty()) {
            return Optional.empty();
        }

        switch (type) {
            case BOOLEAN:
                return Optional.of(value.get().boolValue());
            case NUMBER:
                return Optional.of(value.get().doubleValue());
            case JSON_NODE:
                return Optional.of(value.get().jsonNodeValue());
            default:
                return Optional.of(value.get().stringValue());
        }
    }

    /**
     * This function will return string assignment value
     * 
     * @param subjectKey
     * @param experimentKey
     * @param subjectAttributes
     * @return
     */
    public Optional<String> getAssignment(String subjectKey, String experimentKey,
            EppoAttributes subjectAttributes) {
        return this.getStringAssignment(subjectKey, experimentKey, subjectAttributes);
    }

    /**
     * This function will return string assignment value without passing
     * subjectAttributes
     * 
     * @param subjectKey
     * @param experimentKey
     * @return
     */
    public Optional<String> getAssignment(String subjectKey, String experimentKey) {
        return this.getStringAssignment(subjectKey, experimentKey, new EppoAttributes());
    }

    /**
     * Maps a subject to a variation for a given flag/experiment.
     *
     * @param subjectKey identifier of the experiment subject, for example a user ID.
     * @param flagKey flagKey feature flag, bandit, or experiment identifier
     * @return the variation string assigned to the subject, or null if an unrecoverable error was encountered.
     */
    public Optional<String> getStringAssignment(String subjectKey, String flagKey) {
        return this.getStringAssignment(subjectKey, flagKey, new EppoAttributes());
    }

    /**
     * Maps a subject to a variation for a given flag/experiment.
     *
     * @param subjectKey identifier of the experiment subject, for example a user ID.
     * @param flagKey flagKey feature flag, bandit, or experiment identifier
     * @param subjectAttributes optional attributes associated with the subject, for example name, email,
     *                          account age, etc. The subject attributes are used for evaluating any targeting
     *                          rules as well as weighting assignment choices for bandits.
     * @return the variation string assigned to the subject, or null if an unrecoverable error was encountered.
     */
    public Optional<String> getStringAssignment(String subjectKey, String flagKey,
            EppoAttributes subjectAttributes) {
        return this.getStringAssignment(subjectKey, flagKey, subjectAttributes, Set.of());
    }

    /**
     * Maps a subject to a variation for a given flag/bandit/experiment.
     *
     * @param subjectKey identifier of the experiment subject, for example a user ID.
     * @param flagKey flagKey feature flag, bandit, or experiment identifier
     * @param subjectAttributes optional attributes associated with the subject, for example name, email,
     *                          account age, etc. The subject attributes are used for evaluating any targeting
     *                          rules as well as weighting assignment choices for bandits.
     * @param assignmentOptions used by bandits to know the assignment options (i.e., actions) available. Options are
     *                          given without attributes, as a set of option names.
     * @return the variation string assigned to the subject, or null if an unrecoverable error was encountered.
     */
    public Optional<String> getStringAssignment(
            String subjectKey,
            String flagKey,
            EppoAttributes subjectAttributes,
            Set<String> assignmentOptions
    ) {
        Map<String, EppoAttributes> assignmentOptionsWithAttributes = assignmentOptions.stream()
                .collect(Collectors.toMap(
                        key -> key,
                        value -> new EppoAttributes()
                ));
        return this.getStringAssignment(subjectKey, flagKey, subjectAttributes, assignmentOptionsWithAttributes);
    }

    /**
     * Maps a subject to a variation for a given flag/bandit/experiment.
     *
     * @param subjectKey identifier of the experiment subject, for example a user ID.
     * @param flagKey flagKey feature flag, bandit, or experiment identifier
     * @param subjectAttributes optional attributes associated with the subject, for example name, email,
     *                          account age, etc. The subject attributes are used for evaluating any targeting
     *                          rules as well as weighting assignment choices for bandits.
     * @param assignmentOptions used by bandits to know the assignment options (i.e., actions) available. Options are
     *                          given as a mapping of the option name to the attributes associated with that option.
     * @return the variation string assigned to the subject, or null if an unrecoverable error was encountered.
     */
    public Optional<String> getStringAssignment(
            String subjectKey,
            String flagKey,
            EppoAttributes subjectAttributes,
            Map<String, EppoAttributes> assignmentOptions
    ) {
        @SuppressWarnings("unchecked")
        Optional<String> typedAssignment = (Optional<String>) this.getTypedAssignment(EppoValueType.STRING, subjectKey, flagKey, subjectAttributes, assignmentOptions);
        return typedAssignment;
    }

    /**
     * This function will return boolean assignment value
     * 
     * @param subjectKey
     * @param experimentKey
     * @param subjectAttributes
     * @return
     */
    public Optional<Boolean> getBooleanAssignment(String subjectKey, String experimentKey,
            EppoAttributes subjectAttributes) {
        return (Optional<Boolean>) this.getTypedAssignment(EppoValueType.BOOLEAN, subjectKey, experimentKey, subjectAttributes, null);
    }

    /**
     * This function will return boolean assignment value without passing
     * subjectAttributes
     * 
     * @param subjectKey
     * @param experimentKey
     * @return
     */
    public Optional<Boolean> getBooleanAssignment(String subjectKey, String experimentKey) {
        return this.getBooleanAssignment(subjectKey, experimentKey, new EppoAttributes());
    }

    /**
     * This function will return double assignment value
     * 
     * @param subjectKey
     * @param experimentKey
     * @param subjectAttributes
     * @return
     */
    public Optional<Double> getDoubleAssignment(String subjectKey, String experimentKey,
            EppoAttributes subjectAttributes) {
        return (Optional<Double>) this.getTypedAssignment(EppoValueType.NUMBER, subjectKey, experimentKey, subjectAttributes, null);
    }

    /**
     * This function will return long assignment value without passing
     * subjectAttributes
     * 
     * @param subjectKey
     * @param experimentKey
     * @return
     */
    public Optional<Double> getDoubleAssignment(String subjectKey, String experimentKey) {
        return this.getDoubleAssignment(subjectKey, experimentKey, new EppoAttributes());
    }

    /**
     * This function will return json string assignment value
     * 
     * @param subjectKey
     * @param experimentKey
     * @param subjectAttributes
     * @return
     */
    public Optional<String> getJSONStringAssignment(String subjectKey, String experimentKey,
            EppoAttributes subjectAttributes) {
        return this.getStringAssignment(subjectKey, experimentKey, subjectAttributes);
    }

    /**
     * This function will return json string assignment value without passing
     * subjectAttributes
     * 
     * @param subjectKey
     * @param experimentKey
     * @return
     */
    public Optional<String> getJSONStringAssignment(String subjectKey, String experimentKey) {
        return this.getJSONStringAssignment(subjectKey, experimentKey, new EppoAttributes());
    }

    /**
     * This function will return JSON assignment value
     * 
     * @param subjectKey
     * @param experimentKey
     * @param subjectAttributes
     * @return
     */
    public Optional<JsonNode> getParsedJSONAssignment(String subjectKey, String experimentKey,
            EppoAttributes subjectAttributes) {
        return (Optional<JsonNode>) this.getTypedAssignment(EppoValueType.JSON_NODE, subjectKey, experimentKey,
                subjectAttributes, null);
    }

    /**
     * This function will return JSON assignment value without passing
     * subjectAttributes
     * 
     * @param subjectKey
     * @param experimentKey
     * @return
     */
    public Optional<JsonNode> getParsedJSONAssignment(String subjectKey, String experimentKey) {
        return this.getParsedJSONAssignment(subjectKey, experimentKey, new EppoAttributes());
    }

    /**
     * This function is used to check if the Experiment is in the same
     *
     * @param subjectKey
     * @param experimentKey
     * @param subjectShards
     * @param percentageExposure
     * @return
     */
    private boolean isInExperimentSample(
            String subjectKey,
            String experimentKey,
            int subjectShards,
            float percentageExposure) {
        int shard = Shard.getShard("exposure-" + subjectKey + "-" + experimentKey, subjectShards);
        return shard <= percentageExposure * subjectShards;
    }

    /**
     * This function is used to get assigned variation
     *
     * @param subjectKey
     * @param experimentKey
     * @param subjectShards
     * @param subjectShards
     * @return
     */
    private Variation getAssignedVariation(
            String subjectKey,
            String experimentKey,
            int subjectShards,
            List<Variation> variations) {
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
            ExperimentConfiguration experimentConfiguration) {
        String hexedSubjectKey = Shard.getHex(subjectKey);
        return experimentConfiguration.getTypedOverrides().getOrDefault(hexedSubjectKey, new EppoValue());
    }

    /***
     * This function is used to initialize the Eppo Client
     * 
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
                Constants.REQUEST_TIMEOUT_MILLIS);

        // Create wrapper for fetching experiment configuration
        ExperimentConfigurationRequestor expConfigRequestor = new ExperimentConfigurationRequestor(eppoHttpClient);
        // Create Caching for Experiment Configuration
        CacheHelper cacheHelper = new CacheHelper();
        Cache<String, ExperimentConfiguration> experimentConfigurationCache = cacheHelper
                .createExperimentConfigurationCache(Constants.MAX_CACHE_ENTRIES);
        // Create ExperimentConfiguration Store
        ConfigurationStore configurationStore = ConfigurationStore.init(
                experimentConfigurationCache,
                expConfigRequestor);

        // Stop the polling process of any previously initialized client
        if (EppoClient.instance != null) {
            EppoClient.instance.poller.cancel();
        }

        // Start polling for experiment configurations
        Timer poller = new Timer(true);
        FetchConfigurationsTask fetchConfigurationsTask = new FetchConfigurationsTask(configurationStore, poller,
                Constants.TIME_INTERVAL_IN_MILLIS, Constants.JITTER_INTERVAL_IN_MILLIS);
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
