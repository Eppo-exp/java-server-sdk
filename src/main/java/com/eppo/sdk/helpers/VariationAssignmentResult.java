package com.eppo.sdk.helpers;

import cloud.eppo.rac.dto.EppoAttributes;
import cloud.eppo.rac.dto.Variation;

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
      Integer subjectShards) {
    this.variation = assignedVariation;
    this.subjectKey = subjectKey;
    this.subjectAttributes = subjectAttributes;
    this.flagKey = flagKey;
    this.allocationKey = allocationKey;
    this.experimentKey = experimentKey;
    this.subjectShards = subjectShards;
  }

  public Variation getVariation() {
    return variation;
  }

  public String getSubjectKey() {
    return subjectKey;
  }

  public EppoAttributes getSubjectAttributes() {
    return subjectAttributes;
  }

  public String getFlagKey() {
    return flagKey;
  }

  public String getExperimentKey() {
    return experimentKey;
  }

  public String getAllocationKey() {
    return allocationKey;
  }

  public Integer getSubjectShards() {
    return subjectShards;
  }
}
