package com.eppo.sdk.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class BanditNumericAttributeCoefficients implements AttributeCoefficients {
  private String attributeKey;
  private Double coefficient;
  private Double missingValueCoefficient;

  public double scoreForAttributeValue(EppoValue attributeValue) {
    if (attributeValue == null || attributeValue.isNull()) {
      return missingValueCoefficient;
    }
    if (!attributeValue.isNumeric()) {
      log.warn("Unexpected categorical attribute value for attribute "+attributeKey);
    }
    return coefficient * attributeValue.doubleValue();
  }
}
