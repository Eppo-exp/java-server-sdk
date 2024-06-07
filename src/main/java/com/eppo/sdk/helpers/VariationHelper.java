package com.eppo.sdk.helpers;

import cloud.eppo.ShardUtils;
import cloud.eppo.rac.dto.Variation;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public class VariationHelper {

  public static Variation selectVariation(
      String inputKey, int subjectShards, List<Variation> variations) {
    int shard = ShardUtils.getShard(inputKey, subjectShards);

    Optional<Variation> variation =
        variations.stream()
            .filter(config -> ShardUtils.isShardInRange(shard, config.getShardRange()))
            .findFirst();

    if (!variation.isPresent()) {
      throw new NoSuchElementException(
          "Variation shards configured incorrectly for input " + inputKey);
    }

    return variation.get();
  }

  public static double variationProbability(Variation variation, int subjectShards) {
    return (double) (variation.getShardRange().getEnd() - variation.getShardRange().getStart())
        / subjectShards;
  }
}
