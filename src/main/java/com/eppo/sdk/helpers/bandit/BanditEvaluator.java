package com.eppo.sdk.helpers.bandit;

import com.eppo.sdk.dto.*;
import com.eppo.sdk.helpers.Shard;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BanditEvaluator {

    public static List<Variation> evaluateBanditActions(
        String experimentKey,
        BanditParameters modelParameters,
        Map<String, EppoAttributes> actions,
        String subjectKey,
        EppoAttributes subjectAttributes,
        int subjectShards
    ) {
        String modelName = modelParameters != null
          ? modelParameters.getModelName()
          : RandomBanditModel.MODEL_IDENTIFIER; // Default to random model for unknown bandits

        BanditModel model = BanditModelFactory.build(modelName);
        Map<String, Double> actionWeights = model.weighActions(modelParameters, actions, subjectAttributes);
        List<String> shuffledActions = shuffleActions(actions.keySet(), experimentKey, subjectKey);
        return generateVariations(shuffledActions, actionWeights, subjectShards);
    }

    private static List<String> shuffleActions(Set<String> actionKeys, String experimentKey, String subjectKey) {
        // Shuffle randomly (but deterministically) using a hash, tie-breaking with name
        return actionKeys
            .stream()
            .sorted(Comparator.comparingInt((String actionKey) -> hashToPositiveInt(experimentKey, subjectKey, actionKey)).thenComparing(actionKey -> actionKey))
            .collect(Collectors.toList());
    }

    private static int hashToPositiveInt(String experimentKey, String subjectKey, String actionKey) {
        int SHUFFLE_SHARDS = 10000;
        return Shard.getShard(experimentKey+"-"+subjectKey+"-"+actionKey, SHUFFLE_SHARDS);
    }

    private static List<Variation> generateVariations(List<String> shuffledActions, Map<String, Double>  actionWeights, int subjectShards) {

        final AtomicInteger lastShard = new AtomicInteger(0);

        List<Variation> variations = shuffledActions.stream().map(actionName -> {
            double weight = actionWeights.get(actionName);
            int numShards = Double.valueOf(Math.floor(weight * subjectShards)).intValue();
            int shardStart = lastShard.get();
            int shardEnd = shardStart + numShards;
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
        lastVariation.getShardRange().end = Math.max(lastVariation.getShardRange().end, subjectShards);

        return variations;
    }
}
