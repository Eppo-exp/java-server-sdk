package com.eppo.sdk.helpers;

import cloud.eppo.rac.dto.*;
import cloud.eppo.rac.exception.ExperimentConfigurationNotFound;
import cloud.eppo.rac.exception.NetworkException;
import cloud.eppo.rac.exception.NetworkRequestNotAllowed;
import java.util.Map;
import java.util.Optional;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Configuration Store Class */
public class ConfigurationStore {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationStore.class);
  Cache<String, ExperimentConfiguration> experimentConfigurationCache;
  Cache<String, BanditParameters> banditParametersCache;
  ConfigurationRequestor<ExperimentConfigurationResponse> experimentConfigurationRequestor;
  ConfigurationRequestor<BanditParametersResponse> banditParametersRequestor;
  static ConfigurationStore instance = null;

  public ConfigurationStore(
      Cache<String, ExperimentConfiguration> experimentConfigurationCache,
      ConfigurationRequestor<ExperimentConfigurationResponse> experimentConfigurationRequestor,
      Cache<String, BanditParameters> banditParametersCache,
      ConfigurationRequestor<BanditParametersResponse> banditParametersRequestor) {
    this.experimentConfigurationRequestor = experimentConfigurationRequestor;
    this.experimentConfigurationCache = experimentConfigurationCache;
    this.banditParametersCache = banditParametersCache;
    this.banditParametersRequestor = banditParametersRequestor;
  }

  public static ConfigurationStore init(
      Cache<String, ExperimentConfiguration> experimentConfigurationCache,
      ConfigurationRequestor<ExperimentConfigurationResponse> experimentConfigurationRequestor,
      Cache<String, BanditParameters> banditParametersCache,
      ConfigurationRequestor<BanditParametersResponse> banditParametersRequestor) {
    if (ConfigurationStore.instance == null) {
      ConfigurationStore.instance =
          new ConfigurationStore(
              experimentConfigurationCache,
              experimentConfigurationRequestor,
              banditParametersCache,
              banditParametersRequestor);
    }
    instance.experimentConfigurationCache.clear();
    return ConfigurationStore.instance;
  }

  /** This function is used to get initialized instance */
  public static ConfigurationStore getInstance() {
    return ConfigurationStore.instance;
  }

  /**
   * This function is used to set experiment configuration to cache
   *
   * @param experimentConfiguration
   */
  protected void setExperimentConfiguration(
      String key, ExperimentConfiguration experimentConfiguration) {
    this.experimentConfigurationCache.put(key, experimentConfiguration);
  }

  /**
   * This function is used to fetch experiment configuration
   *
   * @throws ExperimentConfigurationNotFound
   */
  public ExperimentConfiguration getExperimentConfiguration(String key)
      throws ExperimentConfigurationNotFound {
    try {
      return this.experimentConfigurationCache.get(key);
    } catch (Exception e) {
      throw new ExperimentConfigurationNotFound("Experiment configuration not found!");
    }
  }

  public BanditParameters getBanditParameters(String banditKey) {
    return this.banditParametersCache.get(banditKey);
  }

  /**
   * This function is used to set experiment configuration int the cache
   *
   * @throws NetworkException
   * @throws NetworkRequestNotAllowed
   */
  public void fetchAndSetExperimentConfiguration()
      throws NetworkException, NetworkRequestNotAllowed {
    Optional<ExperimentConfigurationResponse> response =
        this.experimentConfigurationRequestor.fetchConfiguration();

    boolean loadBandits = false;
    if (response.isPresent()) {
      for (Map.Entry<String, ExperimentConfiguration> entry :
          response.get().getFlags().entrySet()) {
        ExperimentConfiguration configuration = entry.getValue();
        this.setExperimentConfiguration(entry.getKey(), configuration);
        boolean hasBanditVariation =
            configuration.getAllocations().values().stream()
                .anyMatch(
                    a ->
                        a.getVariations().stream()
                            .anyMatch(
                                v -> v.getAlgorithmType() == AlgorithmType.CONTEXTUAL_BANDIT));

        if (configuration.isEnabled() && hasBanditVariation) {
          loadBandits = true;
        }
      }
    }

    if (loadBandits) {
      Optional<BanditParametersResponse> banditResponse =
          this.banditParametersRequestor.fetchConfiguration();
      if (!banditResponse.isPresent() || banditResponse.get().getBandits() == null) {
        log.warn("Unexpected empty bandit parameter response");
        return;
      }
      for (Map.Entry<String, BanditParameters> entry :
          banditResponse.get().getBandits().entrySet()) {
        this.banditParametersCache.put(entry.getKey(), entry.getValue());
      }
    }
  }
}
