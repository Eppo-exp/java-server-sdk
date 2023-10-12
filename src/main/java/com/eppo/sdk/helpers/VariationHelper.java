package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.Variation;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class VariationHelper {

    static public Variation selectVariation(String inputKey, int subjectShards, List<Variation> variations) {
        int shard = Shard.getShard(inputKey, subjectShards);

        Optional<Variation> variation = variations.stream()
                .filter(config -> Shard.isShardInRange(shard, config.getShardRange()))
                .findFirst();

        if (variation.isEmpty()) {
            throw new NoSuchElementException("Variation shards configured incorrectly for input "+inputKey);
        }

        return variation.get();
    }

    static public float variationProbability(Variation variation, int subjectShards) {
        return (float)(variation.getShardRange().end - variation.getShardRange().start + 1) / subjectShards;
    }
}
