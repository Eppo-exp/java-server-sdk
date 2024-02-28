package com.eppo.sdk.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum AlgorithmType {
    CONSTANT,
    CONTEXTUAL_BANDIT,
    OVERRIDE;

    @JsonCreator
    public static AlgorithmType forValues(String value) {
        return Arrays.stream(AlgorithmType.values())
                .filter(a -> a.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(null);
    }
}
