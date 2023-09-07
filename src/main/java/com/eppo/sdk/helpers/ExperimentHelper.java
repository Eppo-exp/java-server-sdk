package com.eppo.sdk.helpers;

public class ExperimentHelper {
    static public String generateKey(
            String flagKey,
            String allocationKey
    ) {
        return flagKey + '-' + allocationKey;
    }
}
