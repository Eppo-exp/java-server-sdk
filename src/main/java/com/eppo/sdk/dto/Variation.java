package com.eppo.sdk.dto;

import lombok.Data;

/**
 * Experiment's Variation Class
 */
@Data
public class Variation {
    private String name;
    private EppoValue typedValue;
    private ShardRange shardRange;
    private AlgorithmType algorithmType;
}
