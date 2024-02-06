package com.eppo.sdk.helpers;

import java.util.Timer;
import java.util.TimerTask;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FetchConfigurationsTask extends TimerTask {

  private ConfigurationStore configurationStore;
  private Timer timer;
  private long intervalInMillis;
  private long jitterInMillis;

  @Override
  public void run() {
    configurationStore.fetchAndSetExperimentConfiguration();
    // TODO: retry on failed fetches
    long delay = this.intervalInMillis - (long)(Math.random() * this.jitterInMillis);
    FetchConfigurationsTask nextTask = new FetchConfigurationsTask(configurationStore, timer, intervalInMillis, jitterInMillis);
    timer.schedule(nextTask, delay);
  }
}
