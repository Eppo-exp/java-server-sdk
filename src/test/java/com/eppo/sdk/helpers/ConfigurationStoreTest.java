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
            ExperimentConfigurationRequestor requestor
    ) {
        return new ConfigurationStore(experimentConfigurationCache, requestor);
    }

    Cache<String, ExperimentConfiguration> createExperimentConfigurationCache(int maxEntries) {
        CacheHelper cacheHelper = new CacheHelper();
        return cacheHelper.createExperimentConfigurationCache(maxEntries);
    }


    @DisplayName("Test ConfigurationStore.setExperimentConfiguration()")
    @Test()
    void testSetExperimentConfiguration() {
        Cache<String, ExperimentConfiguration> cache = createExperimentConfigurationCache(10);
        ExperimentConfigurationRequestor requestor = Mockito.mock(ExperimentConfigurationRequestor.class);

        ConfigurationStore store = createConfigurationStore(cache, requestor);
        store.setExperimentConfiguration("key1", new ExperimentConfiguration());

        Assertions.assertInstanceOf(ExperimentConfiguration.class, store.getExperimentConfiguration("key1"));
    }
}