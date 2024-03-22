package com.eppo.sdk.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BanditCoefficients {
  private String actionKey;
  private Double intercept;
  private Map<String, BanditNumericAttributeCoefficients> subjectNumericCoefficients;
  private Map<String, BanditCategoricalAttributeCoefficients> subjectCategoricalCoefficients;
  private Map<String, BanditNumericAttributeCoefficients> actionNumericCoefficients;
  private Map<String, BanditCategoricalAttributeCoefficients> actionCategoricalCoefficients;
}
