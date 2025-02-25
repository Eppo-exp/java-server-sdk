package cloud.eppo;

import cloud.eppo.api.IAssignmentCache;
import cloud.eppo.cache.ExpiringInMemoryAssignmentCache;
import cloud.eppo.cache.LRUInMemoryAssignmentCache;
import cloud.eppo.logging.AssignmentLogger;
import cloud.eppo.logging.BanditLogger;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.Nullable;
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

  private static final boolean DEFAULT_IS_GRACEFUL_MODE = true;
  private static final boolean DEFAULT_FORCE_REINITIALIZE = false;
  private static final long DEFAULT_POLLING_INTERVAL_MS = 30 * 1000;
  private static final long DEFAULT_JITTER_INTERVAL_RATIO = 10;

  private static EppoClient instance;

  public static EppoClient getInstance() {
    if (instance == null) {
      throw new IllegalStateException("Eppo SDK has not been initialized");
    }
    return instance;
  }

  private EppoClient(
      String apiKey,
      String sdkName,
      String sdkVersion,
      @Nullable String baseUrl,
      @Nullable AssignmentLogger assignmentLogger,
      @Nullable BanditLogger banditLogger,
      boolean isGracefulMode,
      @Nullable IAssignmentCache assignmentCache,
      @Nullable IAssignmentCache banditAssignmentCache) {
    super(
        apiKey,
        sdkName,
        sdkVersion,
        null,
        baseUrl,
        assignmentLogger,
        banditLogger,
        null,
        isGracefulMode,
        false,
        true,
        null,
        assignmentCache,
        banditAssignmentCache);
  }

  /** Builder pattern to initialize the EppoClient singleton */
  public static class Builder {
    private String apiKey;
    private AssignmentLogger assignmentLogger;
    private BanditLogger banditLogger;
    private boolean isGracefulMode = DEFAULT_IS_GRACEFUL_MODE;
    private boolean forceReinitialize = DEFAULT_FORCE_REINITIALIZE;
    private long pollingIntervalMs = DEFAULT_POLLING_INTERVAL_MS;
    private String apiBaseUrl = null;

    // Assignment and bandit caching on by default. To disable, call
    // `builder.assignmentCache(null).banditAssignmentCache(null);`
    private IAssignmentCache assignmentCache = new LRUInMemoryAssignmentCache(100);
    private IAssignmentCache banditAssignmentCache =
        new ExpiringInMemoryAssignmentCache(10, TimeUnit.MINUTES);

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
     * Overrides the base URL from where the SDK fetches configurations. This typically should not
     * be explicitly set so that the default API URL is used.
     */
    public Builder apiBaseUrl(String apiBaseUrl) {
      this.apiBaseUrl = apiBaseUrl;
      return this;
    }

    public Builder assignmentCache(IAssignmentCache assignmentCache) {
      this.assignmentCache = assignmentCache;
      return this;
    }

    public Builder banditAssignmentCache(IAssignmentCache banditAssignmentCache) {
      this.banditAssignmentCache = banditAssignmentCache;
      return this;
    }

    public EppoClient buildAndInit() {
      AppDetails appDetails = AppDetails.getInstance();
      String sdkName = appDetails.getName();
      String sdkVersion = appDetails.getVersion();

      if (instance != null) {
        // Stop any active polling.
        instance.stopPolling();
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
              apiBaseUrl,
              assignmentLogger,
              banditLogger,
              isGracefulMode,
              assignmentCache,
              banditAssignmentCache);

      // Fetch first configuration
      instance.loadConfiguration();

      // start polling, if enabled.
      if (pollingIntervalMs > 0) {
        instance.startPolling(pollingIntervalMs, pollingIntervalMs / DEFAULT_JITTER_INTERVAL_RATIO);
      }

      return instance;
    }
  }
}
