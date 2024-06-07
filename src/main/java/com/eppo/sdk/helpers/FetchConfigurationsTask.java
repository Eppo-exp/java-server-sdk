package com.eppo.sdk.helpers;

import java.util.Timer;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchConfigurationsTask extends TimerTask {
  private static final Logger log = LoggerFactory.getLogger(FetchConfigurationsTask.class);
  private final ConfigurationStore configurationStore;
  private final Timer timer;
  private final long intervalInMillis;
  private final long jitterInMillis;

  public FetchConfigurationsTask(
      ConfigurationStore configurationStore,
      Timer timer,
      long intervalInMillis,
      long jitterInMillis) {
    this.configurationStore = configurationStore;
    this.timer = timer;
    this.intervalInMillis = intervalInMillis;
    this.jitterInMillis = jitterInMillis;
  }

  @Override
  public void run() {
    // Uncaught runtime exceptions will prevent this task from being rescheduled.
    // As a result, the SDK will continue functioning using the in-memory cache, but will never
    // attempt
    // to synchronize with Eppo Cloud again.
    // TODO: retry on failed fetches
    try {
      configurationStore.fetchAndSetExperimentConfiguration();
    } catch (Exception e) {
      log.error("[Eppo SDK] Error fetching experiment configuration", e);
    }

    long delay = this.intervalInMillis - (long) (Math.random() * this.jitterInMillis);
    FetchConfigurationsTask nextTask =
        new FetchConfigurationsTask(configurationStore, timer, intervalInMillis, jitterInMillis);
    timer.schedule(nextTask, delay);
  }
}
