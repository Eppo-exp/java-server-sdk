package com.eppo.sdk.helpers;

import com.eppo.sdk.constants.Constants;
import com.eppo.sdk.dto.ExperimentConfiguration;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

/**
 * CacheHelper class
 */
public class CacheHelper {
    private CacheManager cacheManager;

    public CacheHelper() {
        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        this.cacheManager.init();
    }

    /**
     * Create caching for Experiment Configuration
     *
     * @param maxEntries
     * @return
     */
    public Cache<String, ExperimentConfiguration> createExperimentConfigurationCache(int maxEntries) {
        return this.cacheManager.createCache(
                Constants.EXPERIMENT_CONFIGURATION_CACHE_KEY,
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(
                                String.class, ExperimentConfiguration.class,
                                ResourcePoolsBuilder.heap(maxEntries)
                        )
        );
    }
}
