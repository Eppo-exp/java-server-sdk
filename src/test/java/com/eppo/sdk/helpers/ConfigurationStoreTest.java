package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.ExperimentConfiguration;
import org.ehcache.Cache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConfigurationStoreTest {

    ConfigurationStore createConfigurationStore(
            Cache<String, ExperimentConfiguration> experimentConfigurationCache,
            ConfigurationRequestor requestor
    ) {
        return new ConfigurationStore(
          experimentConfigurationCache,
          requestor,
          // This test doesn't check bandit cache
          null,
          null
        );
    }

    Cache<String, ExperimentConfiguration> createExperimentConfigurationCache(int maxEntries) {
        CacheHelper cacheHelper = new CacheHelper();
        return cacheHelper.createExperimentConfigurationCache(maxEntries);
    }


    @DisplayName("Test ConfigurationStore.setExperimentConfiguration()")
    @Test()
    void testSetExperimentConfiguration() {
        Cache<String, ExperimentConfiguration> cache = createExperimentConfigurationCache(10);
        ConfigurationRequestor requestor = Mockito.mock(ConfigurationRequestor.class);

        ConfigurationStore store = createConfigurationStore(cache, requestor);
        store.setExperimentConfiguration("key1", new ExperimentConfiguration());

        Assertions.assertInstanceOf(ExperimentConfiguration.class, store.getExperimentConfiguration("key1"));
    }
}
