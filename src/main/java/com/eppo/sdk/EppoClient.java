package com.eppo.sdk;

import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.*;
import com.eppo.sdk.exception.EppoClientIsNotInitializedException;
import com.eppo.sdk.exception.InvalidInputException;
import com.eppo.sdk.helpers.*;
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
    protected Optional<EppoValue> getAssignmentValue(
            String subjectKey,
            String flagKey,
            EppoAttributes subjectAttributes,
            Map<String, EppoAttributes> actionsWithAttributes
    ) {
        Optional<Variation> assignedVariation = getAssignmentVariation(
                subjectKey,
                flagKey,
                subjectAttributes,
                actionsWithAttributes
        );

        if (assignedVariation.isPresent()) {
            return Optional.of(assignedVariation.get().getTypedValue());
        }

        return Optional.empty();
    }

    /**
     * Returns the assigned variation.
     *
     * @param subjectKey
     * @param flagKey
     * @param subjectAttributes
     * @return
     */
    public Optional<Variation> getAssignmentVariation(
            String subjectKey,
            String flagKey,
            EppoAttributes subjectAttributes
    ) {
        return getAssignmentVariation(subjectKey, flagKey, subjectAttributes, null);
    }

    /**
     * Returns the assigned variation.
     *
     * @param subjectKey
     * @param flagKey
     * @param subjectAttributes
     * @param actionsWithAttributes
     * @return
     */
    protected Optional<Variation> getAssignmentVariation(
            String subjectKey,
            String flagKey,
            EppoAttributes subjectAttributes,
            Map<String, EppoAttributes> actionsWithAttributes
    ) {
        // Validate Input Values
        InputValidator.validateNotBlank(subjectKey, "Invalid argument: subjectKey cannot be blank");
        InputValidator.validateNotBlank(flagKey, "Invalid argument: flagKey cannot be blank");

        VariationAssignmentResult assignmentResult = this.getAssignedVariation(flagKey, subjectKey, subjectAttributes);

        if (assignmentResult == null) {
            return Optional.empty();
        }

        Variation assignedVariation = assignmentResult.getVariation();
        Optional<EppoValue> assignmentValue = Optional.of(assignedVariation.getTypedValue());

        // Below is used for logging
        String experimentKey = assignmentResult.getExperimentKey();
        String allocationKey = assignmentResult.getAllocationKey();
        String assignedVariationString = assignedVariation.getTypedValue().stringValue();
        AlgorithmType algorithmType = assignedVariation.getAlgorithmType();

        if (algorithmType == AlgorithmType.OVERRIDE) {
            // Assigned variation was from an override; return its value without logging
            return Optional.of(assignedVariation);
        } else if (algorithmType == AlgorithmType.CONTEXTUAL_BANDIT) {
            // Assigned variation is a bandit; need to use the bandit to determine its value
            Optional<EppoValue> banditValue = this.determineAndLogBanditAction(assignmentResult, actionsWithAttributes);
            assignedVariation.setTypedValue(banditValue.orElse(null));
        }

        // Log the assignment
        try {
            this.eppoClientConfig.getAssignmentLogger()
                    .logAssignment(new AssignmentLogData(
                            experimentKey,
                            flagKey,
                            allocationKey,
                            assignedVariationString,
                            subjectKey,
                            subjectAttributes));
        } catch (Exception e) {
            log.warn("Error logging assignment", e);
        }

        return Optional.of(assignedVariation);
    }

    private VariationAssignmentResult getAssignedVariation(String flagKey, String subjectKey, EppoAttributes subjectAttributes) {

        // Fetch Experiment Configuration
        ExperimentConfiguration configuration = this.configurationStore.getExperimentConfiguration(flagKey);
        if (configuration == null) {
            log.warn("[Eppo SDK] No configuration found for key: " + flagKey);
            return null;
        }

        // Check if subject has override variations
        EppoValue subjectVariationOverride = this.getSubjectVariationOverride(subjectKey, configuration);
        if (!subjectVariationOverride.isNull()) {
            // Create placeholder variation for the override
            Variation overrideVariation = new Variation();
            overrideVariation.setTypedValue(subjectVariationOverride);
            overrideVariation.setAlgorithmType(AlgorithmType.OVERRIDE);
            return new VariationAssignmentResult(overrideVariation);
        }

        // Check if disabled
        if (!configuration.isEnabled()) {
            log.info("[Eppo SDK] No assigned variation because the experiment or feature flag {} is disabled", flagKey);
            return null;
        }

        // Find matched rule
        Optional<Rule> rule = RuleValidator.findMatchingRule(subjectAttributes, configuration.getRules());
        if (!rule.isPresent()) {
            log.info("[Eppo SDK] No assigned variation. The subject attributes did not match any targeting rules");
            return null;
        }

        // Check if in experiment sample
        String allocationKey = rule.get().getAllocationKey();
        Allocation allocation = configuration.getAllocation(allocationKey);
        int subjectShards = configuration.getSubjectShards();
        if (!this.isInExperimentSample(subjectKey, flagKey, subjectShards,
          allocation.getPercentExposure())) {
            log.info("[Eppo SDK] No assigned variation. The subject is not part of the sample population");
            return null;
        }

        List<Variation> variations = allocation.getVariations();

        String experimentKey = ExperimentHelper.generateKey(flagKey, allocationKey); // Used for logging

        // Get assigned variation
        String assignmentKey = "assignment-" + subjectKey + "-" + flagKey;
        Variation assignedVariation = VariationHelper.selectVariation(assignmentKey, subjectShards, variations);

        return new VariationAssignmentResult(
          assignedVariation,
          subjectKey,
          subjectAttributes,
          flagKey,
          allocationKey,
          experimentKey,
          subjectShards
        );
    }

    private Optional<EppoValue> determineAndLogBanditAction(VariationAssignmentResult assignmentResult, Map<String, EppoAttributes> assignmentOptions) {
        String banditName = assignmentResult.getVariation().getTypedValue().stringValue();

        String banditKey = assignmentResult.getVariation().getTypedValue().stringValue();
        BanditParameters banditParameters = this.configurationStore.getBanditParameters(banditKey);

        List<Variation> actionVariations = BanditEvaluator.evaluateBanditActions(
          assignmentResult.getExperimentKey(),
          banditParameters,
          assignmentOptions,
          assignmentResult.getSubjectKey(),
          assignmentResult.getSubjectAttributes(),
          assignmentResult.getSubjectShards()
        );

        String actionSelectionKey = "bandit-" + banditName + "-" + assignmentResult.getSubjectKey() + "-" + assignmentResult.getFlagKey();
        Variation selectedAction = VariationHelper.selectVariation(actionSelectionKey, assignmentResult.getSubjectShards(), actionVariations);

        EppoValue actionValue = selectedAction.getTypedValue();
        String actionString = actionValue.stringValue();
        double actionProbability = VariationHelper.variationProbability(selectedAction, assignmentResult.getSubjectShards());

        if (this.eppoClientConfig.getBanditLogger() != null) {
            // Do bandit-specific logging

            String modelVersionToLog = "uninitialized"; // Default model "version" if we have not seen this bandit before or don't have model parameters for it
            if (banditParameters != null) {
                modelVersionToLog = banditParameters.getModelName() + " " + banditParameters.getModelVersion();
            }

            // Get the action-related attributes
            EppoAttributes actionAttributes = new EppoAttributes();
            if (assignmentOptions != null && !assignmentOptions.isEmpty()) {
                actionAttributes = assignmentOptions.get(actionString);
            }

            Map<String, Double> subjectNumericAttributes = numericAttributes(assignmentResult.getSubjectAttributes());
            Map<String, String> subjectCategoricalAttributes = categoricalAttributes(assignmentResult.getSubjectAttributes());
            Map<String, Double> actionNumericAttributes = numericAttributes(actionAttributes);
            Map<String, String> actionCategoricalAttributes = categoricalAttributes(actionAttributes);

            this.eppoClientConfig.getBanditLogger().logBanditAction(new BanditLogData(
              assignmentResult.getExperimentKey(),
              banditName,
              assignmentResult.getSubjectKey(),
              actionString,
              actionProbability,
              modelVersionToLog,
              subjectNumericAttributes,
              subjectCategoricalAttributes,
              actionNumericAttributes,
              actionCategoricalAttributes
            ));
        }

        return Optional.of(actionValue);
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
            Map<String, EppoAttributes> actionsWithAttributes
    ) {
        try {
            Optional<EppoValue> value = this.getAssignmentValue(subjectKey, experimentKey, subjectAttributes, actionsWithAttributes);
            if (!value.isPresent()) {
                return Optional.empty();
            }

            EppoValue eppoValue = value.get();

            switch (type) {
                case NUMBER:
                    return Optional.of(eppoValue.doubleValue());
                case BOOLEAN:
                    return Optional.of(eppoValue.boolValue());
                case ARRAY_OF_STRING:
                    return Optional.of(eppoValue.arrayValue());
                case JSON_NODE:
                    return Optional.of(eppoValue.jsonNodeValue());
                default: // strings and null
                    return Optional.of(eppoValue.stringValue());
            }
        } catch (Exception e) {
          // if graceful mode
          if (this.eppoClientConfig.isGracefulMode()) {
            log.warn("[Eppo SDK] Error getting assignment value: " + e.getMessage());
            return Optional.empty();
          }
          throw e;
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
        Optional<String> typedAssignment = (Optional<String>) this.getTypedAssignment(EppoValueType.STRING, subjectKey, flagKey, subjectAttributes, new HashMap<>());
        return typedAssignment;
    }

    /**
     * Maps a subject to a variation for a given flag/experiment that has bandit variation.
     *
     * @param subjectKey identifier of the experiment subject, for example a user ID.
     * @param flagKey flagKey feature flag, bandit, or experiment identifier
     * @param subjectAttributes optional attributes associated with the subject, for example name, email,
     *                          account age, etc. The subject attributes are used for evaluating any targeting
     *                          rules as well as weighting assignment choices for bandits.
     * @param actions used by bandits to know the actions (potential assignments) available.
     * @return the variation string assigned to the subject, or null if an unrecoverable error was encountered.
     */
    public Optional<String> getBanditAssignment(
      String subjectKey,
      String flagKey,
      EppoAttributes subjectAttributes,
      Set<String> actions
    ) {
        Map<String, EppoAttributes> actionsWithEmptyAttributes = actions.stream()
          .collect(Collectors.toMap(
            key -> key,
            value -> new EppoAttributes()
          ));
        return this.getBanditAssignment(subjectKey, flagKey, subjectAttributes, actionsWithEmptyAttributes);
    }

    /**
     * Maps a subject to a variation for a given flag/experiment that contains a bandit variation.
     *
     * @param subjectKey identifier of the experiment subject, for example a user ID.
     * @param flagKey flagKey feature flag, bandit, or experiment identifier
     * @param subjectAttributes optional attributes associated with the subject, for example name, email,
     *                          account age, etc. The subject attributes are used for evaluating any targeting
     *                          rules as well as weighting assignment choices for bandits.
     * @param actionsWithAttributes used by bandits to know the actions (assignment options) available and any
     *                              attributes associated with that option.
     * @return the variation string assigned to the subject, or null if an unrecoverable error was encountered.
     */
    public Optional<String> getBanditAssignment(
      String subjectKey,
      String flagKey,
      EppoAttributes subjectAttributes,
      Map<String, EppoAttributes> actionsWithAttributes
    ) {
        @SuppressWarnings("unchecked")
        Optional<String> typedAssignment = (Optional<String>) this.getTypedAssignment(EppoValueType.STRING, subjectKey, flagKey, subjectAttributes, actionsWithAttributes);
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
            double percentageExposure) {
        int shard = Shard.getShard("exposure-" + subjectKey + "-" + experimentKey, subjectShards);
        return shard <= percentageExposure * subjectShards;
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
        return experimentConfiguration.getTypedOverrides().getOrDefault(hexedSubjectKey, EppoValue.nullValue());
    }

    /***
     * Logs an action taken that was not selected by the bandit.
     * Useful for full transparency on what users experienced.
     * @param subjectKey subjectKey identifier of the experiment subject, for example a user ID.
     * @param flagKey feature flag, bandit, or experiment identifier
     * @param subjectAttributes optional attributes associated with the subject, for example name, email, account age, etc.
     * @param actionString name of the action taken for the subject
     * @param actionAttributes attributes associated with the given action
     * @return null if no exception was encountered by logging; otherwise, the encountered exception
     */
    public Exception logNonBanditAction(
        String subjectKey,
        String flagKey,
        EppoAttributes subjectAttributes,
        String actionString,
        EppoAttributes actionAttributes
    ) {
        Exception loggingException = null;
        try {
            VariationAssignmentResult assignmentResult = this.getAssignedVariation(flagKey, subjectKey, subjectAttributes);

            if (assignmentResult == null) {
                // No bandit at play
                return null;
            }

            String variationValue = assignmentResult.getVariation().getTypedValue().toString();

            Map<String, Double> subjectNumericAttributes = numericAttributes(subjectAttributes);
            Map<String, String> subjectCategoricalAttributes = categoricalAttributes(subjectAttributes);
            Map<String, Double> actionNumericAttributes = numericAttributes(actionAttributes);
            Map<String, String> actionCategoricalAttributes = categoricalAttributes(actionAttributes);

            this.eppoClientConfig.getBanditLogger().logBanditAction(new BanditLogData(
              assignmentResult.getExperimentKey(),
              variationValue,
              subjectKey,
              actionString,
              null,
              null,
              subjectNumericAttributes,
              subjectCategoricalAttributes,
              actionNumericAttributes,
              actionCategoricalAttributes
            ));
        } catch (Exception ex) {
            loggingException = ex;
        }
        return loggingException;
    }

    private Map<String, Double> numericAttributes(EppoAttributes eppoAttributes) {
        if (eppoAttributes == null) {
            return new HashMap<>();
        }
        return eppoAttributes.entrySet().stream().filter(e -> e.getValue().isNumeric()
        ).collect(Collectors.toMap(
          Map.Entry::getKey,
          e -> e.getValue().doubleValue())
        );
    }

    private Map<String, String> categoricalAttributes(EppoAttributes eppoAttributes) {
        if (eppoAttributes == null) {
            return new HashMap<>();
        }
        return eppoAttributes.entrySet().stream().filter(e -> !e.getValue().isNumeric() && !e.getValue().isNull()
        ).collect(Collectors.toMap(
          Map.Entry::getKey,
          e -> e.getValue().toString())
        );
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

        // Create wrapper for fetching experiment and bandit configuration
        ConfigurationRequestor<ExperimentConfigurationResponse> expConfigRequestor =
          new ConfigurationRequestor<>(ExperimentConfigurationResponse.class, eppoHttpClient, Constants.RAC_ENDPOINT);
        ConfigurationRequestor<BanditParametersResponse> banditParametersRequestor =
          new ConfigurationRequestor<>(BanditParametersResponse.class, eppoHttpClient, Constants.BANDIT_ENDPOINT);
        // Create Caching for Experiment Configuration and Bandit Parameters
        CacheHelper cacheHelper = new CacheHelper();
        Cache<String, ExperimentConfiguration> experimentConfigurationCache = cacheHelper
                .createExperimentConfigurationCache(Constants.MAX_CACHE_ENTRIES);
        Cache<String, BanditParameters> banditParametersCache = cacheHelper
          .createBanditParameterCache(Constants.MAX_CACHE_ENTRIES);
        // Create ExperimentConfiguration Store
        ConfigurationStore configurationStore = ConfigurationStore.init(
                experimentConfigurationCache,
                expConfigRequestor,
                banditParametersCache,
                banditParametersRequestor
        );

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
