package com.eppo.sdk.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BanditModelData {
  private Double gamma;
  private Double defaultActionScore;
  private Double actionProbabilityFloor;
  private Map<String, BanditCoefficients> coefficients;
}
