package com.eppo.sdk;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.api.Configuration;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditLogger;
import com.eppo.sdk.helpers.AppDetails;
import com.eppo.sdk.helpers.FetchConfigurationsTask;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to ingest and use the flag and bandit configurations retrieved from Eppo This class
 * uses the Singleton pattern. First the singleton must be initialized via it's Builder's
 * buildAndInit() method. Then call getInstance() to access the singleton and call methods to get
 * assignments and bandit actions.
 */
public class EppoClient extends BaseEppoClient {
  private static final Logger log = LoggerFactory.getLogger(EppoClient.class);

  private static final String DEFAULT_HOST = "https://fscdn.eppo.cloud";
  private static final boolean DEFAULT_IS_GRACEFUL_MODE = true;
  private static final boolean DEFAULT_FORCE_REINITIALIZE = false;
  private static final long DEFAULT_POLLING_INTERVAL_MS = 30 * 1000;
  private static final long DEFAULT_JITTER_INTERVAL_RATIO = 10;

  private static EppoClient instance;
  private static Timer pollTimer;

  public static EppoClient getInstance() {
    if (instance == null) {
      throw new IllegalStateException("Eppo SDK has not been initialized");
    }
    return instance;
  }

  private EppoClient(
      String apiKey,
      String host,
      String sdkName,
      String sdkVersion,
      AssignmentLogger assignmentLogger,
      BanditLogger banditLogger,
      boolean isGracefulModel,
      CompletableFuture<Configuration> initialConfiguration) {
    super(
        apiKey,
        host,
        sdkName,
        sdkVersion,
        assignmentLogger,
        banditLogger,
        null,
        isGracefulModel,
        false,
        true,
        initialConfiguration);
  }

  /** Stops the client from polling Eppo for updated flag and bandit configurations */
  public static void stopPolling() {
    if (pollTimer != null) {
      pollTimer.cancel();
    }
  }

  /** Builder pattern to initialize the EppoClient singleton */
  public static class Builder {
    private String apiKey;
    private AssignmentLogger assignmentLogger;
    private BanditLogger banditLogger;
    private boolean isGracefulMode = DEFAULT_IS_GRACEFUL_MODE;
    private boolean forceReinitialize = DEFAULT_FORCE_REINITIALIZE;
    private long pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS;
    private String host = DEFAULT_HOST;
    private CompletableFuture<Configuration> initialConfiguration;

    /** Sets the API Key--created within the eppo application--to use. This is required. */
    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    /**
     * Assignment logger to use to record when variations were assigned. This is needed if you want
     * Eppo to analyze experiments controlled by flags.
     */
    public Builder assignmentLogger(AssignmentLogger assignmentLogger) {
      this.assignmentLogger = assignmentLogger;
      return this;
    }

    /**
     * Bandit logger to use to record when a bandit has assigned an action. This is needed if you
     * are using contextual multi-armed bandits.
     */
    public Builder banditLogger(BanditLogger banditLogger) {
      this.banditLogger = banditLogger;
      return this;
    }

    /**
     * Sets the initial graceful mode of the client. When on (which is the default), flag evaluation
     * errors will be caught, and the default value returned. When off, the errors will be rethrown.
     */
    public Builder isGracefulMode(boolean isGracefulMode) {
      this.isGracefulMode = isGracefulMode;
      return this;
    }

    /**
     * Sets whether the singleton client should be recreated if one already has been. fetch an
     * updated configuration. If true, a new client will be instantiated and a new fetch for
     * configurations will be performed. If false (which is the default), initialization will be
     * ignored and the previously initialized client will be used.
     */
    public Builder forceReinitialize(boolean forceReinitialize) {
      this.forceReinitialize = forceReinitialize;
      return this;
    }

    /**
     * Sets how often the client should check for updated configurations, in milliseconds. The
     * default is 30,000 (poll every 30 seconds).
     */
    public Builder pollingIntervalMs(long pollingIntervalMs) {
      this.pollingIntervalMs = pollingIntervalMs;
      return this;
    }

    /**
     * Overrides the host from where it fetches configurations. This typically should not be
     * explicitly set so that the default of the Fastly CDN is used.
     */
    public Builder host(String host) {
      this.host = host;
      return this;
    }

    /** Sets the initial configuration for the client. */
    public Builder initialConfiguration(CompletableFuture<Configuration> initialConfiguration) {
      this.initialConfiguration = initialConfiguration;
      return this;
    }

    public EppoClient buildAndInit() {
      AppDetails appDetails = AppDetails.getInstance();
      String sdkName = appDetails.getName();
      String sdkVersion = appDetails.getVersion();

      if (instance != null) {
        if (forceReinitialize) {
          log.warn(
              "Eppo SDK is already initialized, reinitializing since forceReinitialize is true");
        } else {
          log.warn(
              "Eppo SDK is already initialized, skipping reinitialization since forceReinitialize is false");
          return instance;
        }
      }

      instance =
          new EppoClient(
              apiKey,
              sdkName,
              sdkVersion,
              host,
              assignmentLogger,
              banditLogger,
              isGracefulMode,
              initialConfiguration);

      // Stop any active polling
      stopPolling();

      // Set up polling for experiment configurations
      pollTimer = new Timer(true);
      FetchConfigurationsTask fetchConfigurationsTask =
          new FetchConfigurationsTask(
              () -> instance.loadConfiguration(),
              pollTimer,
              pollingIntervalMs,
              pollingIntervalMs / DEFAULT_JITTER_INTERVAL_RATIO);

      // Kick off the first fetch
      fetchConfigurationsTask.run();

      return instance;
    }
  }
}
