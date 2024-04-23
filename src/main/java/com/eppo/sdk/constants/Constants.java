package com.eppo.sdk.constants;

/**
 * Constants Class
 */
public class Constants {
    /**
     * Base URL
     */
    public static final String DEFAULT_BASE_URL = "https://fscdn.eppo.cloud/api";
    public static final int REQUEST_TIMEOUT_MILLIS = 1000;

    /**
     * Poller Settings
     */


    private static final long MILLISECOND_IN_ONE_SECOND = 1000;

    public static final long
            TIME_INTERVAL_IN_MILLIS = 30 * MILLISECOND_IN_ONE_SECOND; // time interval
    public static final long
            JITTER_INTERVAL_IN_MILLIS = 5 * MILLISECOND_IN_ONE_SECOND;

    /**
     * Cache Settings
     */
    public static final int MAX_CACHE_ENTRIES = 1000;

    /**
     * RAC settings
     */
    public static final String RAC_ENDPOINT = "/randomized_assignment/v3/config";

    public static final String BANDIT_ENDPOINT = "/flag-config/v1/bandits";


    /**
     * Caching Settings
     */
    public static final String EXPERIMENT_CONFIGURATION_CACHE_KEY = "experiment-configuration";
    public static final String BANDIT_PARAMETER_CACHE_KEY = "bandit-parameter";

}
