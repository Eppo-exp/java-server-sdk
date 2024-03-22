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

        if (!variation.isPresent()) {
            throw new NoSuchElementException("Variation shards configured incorrectly for input "+inputKey);
        }

        return variation.get();
    }

    static public double variationProbability(Variation variation, int subjectShards) {
        return (double)(variation.getShardRange().end - variation.getShardRange().start) / subjectShards;
    }
}
