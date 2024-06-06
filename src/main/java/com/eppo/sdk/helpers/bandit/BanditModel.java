package com.eppo.sdk.helpers.bandit;

import cloud.eppo.rac.dto.BanditParameters;
import cloud.eppo.rac.dto.EppoAttributes;

import java.util.Map;

public interface BanditModel {
  Map<String, Double> weighActions(BanditParameters parameters, Map<String, EppoAttributes> actions, EppoAttributes subjectAttributes);
}
