package com.eppo.sdk.dto;

import lombok.Data;

@Data
public class BanditNumericAttributeCoefficients {
  private String attributeKey;
  private Double coefficient;
  private Double missingValueCoefficient;
}
