package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.ShardRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ShardTest {

    ShardRange createShardRange(int start, int end) {
        ShardRange range = new ShardRange();
        range.start = start;
        range.end = end;

        return range;
    }

    @DisplayName("Test Shard.isShardInRange() positive case")
    @Test
    void testIsShardInRangePositiveCase() {
        ShardRange range = createShardRange(10, 20);
        Assertions.assertTrue(Shard.isShardInRange(15, range));
    }

    @DisplayName("Test Shard.isShardInRange() negative case")
    @Test
    void testIsShardInRangeNegativeCase() {
        ShardRange range = createShardRange(10, 20);
        Assertions.assertTrue(Shard.isShardInRange(15, range));
    }

    @DisplayName("Test Shard.getShard()")
    @Test
    void testGetShard() throws Exception {
        final int MAX_SHARD_VALUE = 200;
        int shardValue = Shard.getShard("test-user", MAX_SHARD_VALUE);
        Assertions.assertTrue(shardValue >= 0 & shardValue <= MAX_SHARD_VALUE);
    }
}