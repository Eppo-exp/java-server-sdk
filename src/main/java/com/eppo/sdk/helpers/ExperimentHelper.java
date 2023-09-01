package com.eppo.sdk.helpers;

import java.util.Base64;

public class ExperimentHelper {
    static public String generateKey(
            String flagKey,
            String allocationKey
    ) {
        return flagKey + '-' + Base64.getEncoder().encodeToString(allocationKey.getBytes());
    }
}
