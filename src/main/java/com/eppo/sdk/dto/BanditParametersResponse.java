package com.eppo.sdk.dto;

import com.eppo.sdk.deserializer.BanditsDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class BanditParametersResponse {
    private Date updatedAt;
    @JsonDeserialize(using = BanditsDeserializer.class)
    private Map<String, BanditParameters> bandits;
}
