package com.eppo.sdk.dto;

/**
 * Experiment's Variation Class
 */
public class Variation {
    public EppoValue name;
    public ShardRange shardRange;

    @Override
    public String toString() {
        return "[Name: " + name + "| ShareRange: " + shardRange.toString() + "]";
    }
}
