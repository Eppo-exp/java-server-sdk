package com.eppo.sdk.helpers;

import com.eppo.sdk.dto.ExperimentConfiguration;
import com.eppo.sdk.dto.ExperimentConfigurationResponse;
import org.ehcache.Cache;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

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

    @DisplayName("Test ConfigurationStore.setExperimentConfiguration()")
    @Test()
    void testGetExperimentConfiguration() {
        Cache<String, ExperimentConfiguration> cache = createExperimentConfigurationCache(10);
        ExperimentConfigurationRequestor requestor = Mockito.mock(ExperimentConfigurationRequestor.class);
        Map<String, ExperimentConfiguration> map = new HashMap<>();
        map.put("key1", new ExperimentConfiguration());
        ExperimentConfigurationResponse response = new ExperimentConfigurationResponse();
        response.experiments = map;

        // mocked requestor
        Mockito.when(requestor.fetchExperimentConfiguration()).thenReturn(Optional.of(response));
        Mockito.when(requestor.isRequestAllowed()).thenReturn(true);

        ConfigurationStore store = createConfigurationStore(cache, requestor);
        Assertions.assertInstanceOf(ExperimentConfiguration.class, store.getExperimentConfiguration("key1"));
    }

}