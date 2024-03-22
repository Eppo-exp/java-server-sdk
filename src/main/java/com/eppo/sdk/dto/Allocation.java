package com.eppo.sdk.dto;

import java.util.List;

import lombok.Data;

@Data
public class Allocation {
  private double percentExposure;
  private List<Variation> variations;
}
