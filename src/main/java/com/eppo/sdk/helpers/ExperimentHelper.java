package com.eppo.sdk.helpers;

public class ExperimentHelper {
  public static String generateKey(String flagKey, String allocationKey) {
    return flagKey + '-' + allocationKey;
  }
}
