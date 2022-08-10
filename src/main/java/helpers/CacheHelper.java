package helpers;

import dto.ExperimentConfiguration;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;


public class CacheHelper {
    private CacheManager cacheManager;

    public CacheHelper() {
        this.cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        this.cacheManager.init();
    }

    public Cache<String, ExperimentConfiguration> createExperimentConfigurationCache(int maxEntries) {
        return this.cacheManager.createCache(
                "experiment-configuration",
                CacheConfigurationBuilder
                        .newCacheConfigurationBuilder(String.class, ExperimentConfiguration.class, ResourcePoolsBuilder.heap(maxEntries))
        );
    }
}
