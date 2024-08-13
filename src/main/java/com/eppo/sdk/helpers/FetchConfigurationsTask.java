package com.eppo.sdk.helpers;

import java.util.Timer;
import java.util.TimerTask;

import com.eppo.sdk.EppoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchConfigurationsTask extends TimerTask {
  private static final Logger log = LoggerFactory.getLogger(FetchConfigurationsTask.class);
  private final EppoClient eppoClient;
  private final Timer timer;
  private final long intervalInMillis;
  private final long jitterInMillis;

  public FetchConfigurationsTask(
    EppoClient eppoClient,
      Timer timer,
      long intervalInMillis,
      long jitterInMillis) {
    this.eppoClient = eppoClient;
    this.timer = timer;
    this.intervalInMillis = intervalInMillis;
    this.jitterInMillis = jitterInMillis;
  }

  @Override
  public void run() {
    // TODO: retry on failed fetches
    try {
      eppoClient.refreshConfiguration();
    } catch (Exception e) {
      log.error("[Eppo SDK] Error fetching experiment configuration", e);
    }

    long delay = this.intervalInMillis - (long) (Math.random() * this.jitterInMillis);
    FetchConfigurationsTask nextTask =
        new FetchConfigurationsTask(eppoClient, timer, intervalInMillis, jitterInMillis);
    timer.schedule(nextTask, delay);
  }
}
