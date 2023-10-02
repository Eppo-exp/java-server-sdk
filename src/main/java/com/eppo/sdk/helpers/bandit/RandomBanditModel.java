package com.eppo.sdk.helpers.bandit;

import com.eppo.sdk.dto.EppoAttributes;

import java.util.Map;
import java.util.stream.Collectors;

public class RandomBanditModel implements BanditModel {

    public Map<String, Float> weighActions(Map<String, EppoAttributes> actions, EppoAttributes subjectAttributes) {
        final float weightPerAction = 1 / (float)actions.size();
        return actions.keySet().stream().collect(Collectors.toMap(
            key -> key,
            value -> weightPerAction
        ));
    }
}
