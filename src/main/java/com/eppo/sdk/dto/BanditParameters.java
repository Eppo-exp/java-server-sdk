package com.eppo.sdk.dto;

import java.util.Date;
import lombok.Data;

@Data
public class BanditParameters {
  private String banditKey;
  private Date updatedAt;
  private String model;
  private String modelVersion;
  private BanditModelData modelData;
}
