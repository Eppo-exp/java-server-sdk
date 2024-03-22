package com.eppo.sdk.helpers;

import java.util.Timer;
import java.util.TimerTask;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class FetchConfigurationsTask extends TimerTask {

  private ConfigurationStore configurationStore;
  private Timer timer;
  private long intervalInMillis;
  private long jitterInMillis;

  @Override
  public void run() {
    // Uncaught runtime exceptions will prevent this task from being rescheduled.
    // As a result, the SDK will continue functioning using the in-memory cache, but will never attempt
    // to synchronize with Eppo Cloud again.
    // TODO: retry on failed fetches
    try {
      configurationStore.fetchAndSetExperimentConfiguration();
    } catch (Exception e) {
      log.error("[Eppo SDK] Error fetching experiment configuration", e);
    }

    long delay = this.intervalInMillis - (long) (Math.random() * this.jitterInMillis);
    FetchConfigurationsTask nextTask = new FetchConfigurationsTask(configurationStore, timer, intervalInMillis, jitterInMillis);
    timer.schedule(nextTask, delay);
  }
}
