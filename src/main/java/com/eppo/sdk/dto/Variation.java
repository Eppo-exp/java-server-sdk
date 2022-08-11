package com.eppo.sdk.dto;

public class Variation {
    public String name;
    public ShardRange shardRange;

    @Override
    public String toString() {
        return "[Name: " + name + "| ShareRange: " + shardRange.toString() + "]";
    }
}
