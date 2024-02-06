package com.eppo.sdk.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BanditModelData {
  private Double gamma;
  private Map<String, BanditCoefficients> coefficients;
}
