package com.eppo.sdk.dto;

public interface AttributeCoefficients {

  String getAttributeKey();
  double scoreForAttributeValue(EppoValue attributeValue);
}
