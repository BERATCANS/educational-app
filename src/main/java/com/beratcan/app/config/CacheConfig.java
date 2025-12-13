package com.beratcan.app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache Configuration
 * Static data için caching desteği sağlar
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * In-memory cache manager
     * Filter options ve cities gibi static data için kullanılır
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("filterOptions", "cities");
    }
}
