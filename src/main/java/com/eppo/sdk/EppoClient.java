package com.eppo.sdk;

import cloud.eppo.BaseEppoClient;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditLogger;
import com.eppo.sdk.helpers.AppDetails;
import com.eppo.sdk.helpers.FetchConfigurationsTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;

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
    boolean isGracefulModel
  ) {
    super(apiKey, host, sdkName, sdkVersion, assignmentLogger, banditLogger, isGracefulModel, false);
  }

  public static void stopPolling() {
    if (pollTimer != null) {
      pollTimer.cancel();
    }
  }

  public static class Builder {
    private String apiKey;
    private String host = DEFAULT_HOST;
    private AssignmentLogger assignmentLogger;
    private BanditLogger banditLogger;
    private boolean isGracefulMode = DEFAULT_IS_GRACEFUL_MODE;
    private boolean forceReinitialize = DEFAULT_FORCE_REINITIALIZE;
    private long pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS;

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder assignmentLogger(AssignmentLogger assignmentLogger) {
      this.assignmentLogger = assignmentLogger;
      return this;
    }

    public Builder banditLogger(BanditLogger banditLogger) {
      this.banditLogger = banditLogger;
      return this;
    }

    public Builder isGracefulMode(boolean isGracefulMode) {
      this.isGracefulMode = isGracefulMode;
      return this;
    }

    public Builder forceReinitialize(boolean forceReinitialize) {
      this.forceReinitialize = forceReinitialize;
      return this;
    }

    public Builder pollingIntervalMs(long pollingIntervalMs) {
      this.pollingIntervalMs = pollingIntervalMs;
      return this;
    }

    public EppoClient buildAndInit() {
      AppDetails appDetails = AppDetails.getInstance();
      String sdkName = appDetails.getName();
      String sdkVersion = appDetails.getVersion();

      if (instance != null) {
        if (forceReinitialize) { // TODO: unit test this
          log.warn("Eppo SDK is already initialized, reinitializing since forceReinitialize is true");
        } else {
          log.warn("Eppo SDK is already initialized, skipping reinitialization since forceReinitialize is false");
          return instance;
        }
      }

      instance = new EppoClient(apiKey, sdkName, sdkVersion, host, assignmentLogger, banditLogger, isGracefulMode);

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
