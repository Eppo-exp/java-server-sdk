package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.EppoAttributes;
import com.eppo.sdk.dto.Variation;
import lombok.Getter;

@Getter
public class VariationAssignmentResult {
    private final Variation variation;
    private final String subjectKey;
    private final EppoAttributes subjectAttributes;
    private final String flagKey;
    private final String experimentKey;
    private final String allocationKey;
    private final Integer subjectShards;

    public VariationAssignmentResult(Variation variation) {
        this(variation, null, null, null, null, null, null);
    }
    public VariationAssignmentResult(
      Variation assignedVariation,
      String subjectKey,
      EppoAttributes subjectAttributes,
      String flagKey,
      String allocationKey,
      String experimentKey,
      Integer subjectShards
    ) {
        this.variation = assignedVariation;
        this.subjectKey = subjectKey;
        this.subjectAttributes = subjectAttributes;
        this.flagKey = flagKey;
        this.allocationKey = allocationKey;
        this.experimentKey = experimentKey;
        this.subjectShards = subjectShards;
    }
}
