package com.eppo.sdk.helpers.bandit;

import com.eppo.sdk.dto.EppoAttributes;
import com.eppo.sdk.dto.EppoValue;
import com.eppo.sdk.dto.ShardRange;
import com.eppo.sdk.dto.Variation;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BanditEvaluator {

    public static List<Variation> evaluateBanditVariations(
      String banditKey,
      String modelName,
      Map<String, EppoAttributes> actions,
      String subjectKey,
      EppoAttributes subjectAttributes,
      int subjectShards
    ) {
        BanditModel model = BanditModelFactory.build(modelName);
        List<String> shuffledActions = shuffleVariations(actions.keySet(), banditKey, subjectKey);
        Map<String, Float> actionWeights = model.weighActions(actions, subjectAttributes);
        return generateVariations(shuffledActions, actionWeights, subjectShards);
    }

    private static List<String> shuffleVariations(Set<String> actionKeys, String banditKey, String subjectKey) {
        return actionKeys
          .stream()
          .sorted(Comparator.comparingInt(actionKey -> hashToPositiveInt(subjectKey, banditKey, actionKey)))
          .collect(Collectors.toList());
    }

    private static int hashToPositiveInt(String actionKey, String banditKey, String subjectKey) {
        try {
            // Create MD5 Hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(actionKey.getBytes(StandardCharsets.UTF_8));
            md.update(banditKey.getBytes(StandardCharsets.UTF_8));
            md.update(subjectKey.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();

            // Use the first 4 bytes to create an int and ensure it's positive
            return ByteBuffer.wrap(digest, 0, 4).getInt() & 0x7FFFFFFF;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not generate MD5", e);
        }
    }

    private static List<Variation> generateVariations(List<String> shuffledActions, Map<String, Float>  actionWeights, int subjectShards) {

        final AtomicInteger lastShard = new AtomicInteger(-1);

        List<Variation> variations = shuffledActions.stream().map(actionName -> {
            float weight = actionWeights.get(actionName);
            int numShards = Double.valueOf(Math.floor(weight * subjectShards)).intValue();
            int shardStart = lastShard.get() + 1;
            int shardEnd = shardStart + numShards - 1;
            lastShard.set(shardEnd);

            ShardRange shardRange = new ShardRange();
            shardRange.start = shardStart;
            shardRange.end = shardEnd;

            Variation variation = new Variation();
            variation.setTypedValue(EppoValue.valueOf(actionName));
            variation.setShardRange(shardRange);

            return variation;
        }).collect(Collectors.toList());

        // Pad last shard if needed due to rounding of weights
        Variation lastVariation = variations.get(variations.size() - 1);
        lastVariation.getShardRange().end = Math.max(lastVariation.getShardRange().end, subjectShards - 1);

        return variations;
    }
}
