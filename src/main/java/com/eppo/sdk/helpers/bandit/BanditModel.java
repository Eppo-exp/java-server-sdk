package com.eppo.sdk.helpers.bandit;

import com.eppo.sdk.dto.BanditParameters;
import com.eppo.sdk.dto.EppoAttributes;

import java.util.Map;

public interface BanditModel {

    Map<String, Double> weighActions(BanditParameters parameters, Map<String, EppoAttributes> actions, EppoAttributes subjectAttributes);

}
