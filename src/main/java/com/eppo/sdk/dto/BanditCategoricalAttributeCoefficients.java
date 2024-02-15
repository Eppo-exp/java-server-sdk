package com.eppo.sdk.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Data
public class BanditCategoricalAttributeCoefficients implements AttributeCoefficients {
  private String attributeKey;
  private Double missingValueCoefficient;
  private Map<String, Double> valueCoefficients;

  public double scoreForAttributeValue(EppoValue attributeValue) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (attributeValue.isNumeric()) {
      log.warn("Unexpected numeric attribute value for attribute "+attributeKey);
      return missingValueCoefficient;
    }

    String valueKey = attributeValue.toString();
    Double coefficient = valueCoefficients.get(valueKey);

    // Categorical attributes are treated as one-hot booleans, so it's just the coefficient * 1 when present
    return coefficient != null ? coefficient : missingValueCoefficient;
  }
}
