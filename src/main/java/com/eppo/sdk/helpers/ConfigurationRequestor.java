package com.eppo.sdk.helpers;

import cloud.eppo.rac.exception.InvalidApiKeyException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationRequestor<T> {
  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final Logger log = LoggerFactory.getLogger(ConfigurationRequestor.class);
  private final Class<T> responseClass;
  private final EppoHttpClient eppoHttpClient;
  private final String endpoint;

  public ConfigurationRequestor(
      Class<T> responseClass, EppoHttpClient eppoHttpClient, String endpoint) {
    this.responseClass = responseClass;
    this.eppoHttpClient = eppoHttpClient;
    this.endpoint = endpoint;
  }

  public Optional<T> fetchConfiguration() {
    T config = null;
    try {
      HttpResponse response = this.eppoHttpClient.get(this.endpoint);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == 200) {
        config =
            OBJECT_MAPPER.readValue(EntityUtils.toString(response.getEntity()), this.responseClass);
      }
      if (statusCode == 401) { // unauthorized - invalid API key
        throw new InvalidApiKeyException("Unauthorized: invalid Eppo API key.");
      }
    } catch (InvalidApiKeyException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Unable to Fetch Experiment Configuration: {}", e.getMessage());
    }

    return Optional.ofNullable(config);
  }
}
