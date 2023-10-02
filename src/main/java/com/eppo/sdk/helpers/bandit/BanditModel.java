package com.eppo.sdk.helpers.bandit;

import com.eppo.sdk.dto.EppoAttributes;

import java.util.Map;

public interface BanditModel {

    Map<String, Float> weighActions(Map<String, EppoAttributes> actions, EppoAttributes subjectAttributes);

}
