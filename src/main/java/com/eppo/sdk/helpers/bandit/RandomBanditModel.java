package com.eppo.sdk.helpers.bandit;

import cloud.eppo.rac.dto.BanditParameters;
import cloud.eppo.rac.dto.EppoAttributes;
import java.util.Map;
import java.util.stream.Collectors;

public class RandomBanditModel implements BanditModel {
  public static final String MODEL_IDENTIFIER = "random";

  public Map<String, Double> weighActions(
      BanditParameters parameters,
      Map<String, EppoAttributes> actions,
      EppoAttributes subjectAttributes) {
    final double weightPerAction = 1 / (double) actions.size();
    return actions.keySet().stream()
        .collect(Collectors.toMap(key -> key, value -> weightPerAction));
  }
}
