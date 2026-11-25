package com.commodule.cache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(CacheStore.class)
    @ConditionalOnProperty(
            prefix = "commodule.cache",
            name = "provider",
            havingValue = "memory",
            matchIfMissing = true
    )
    public CacheStore memoryCacheStore(CacheProperties properties) {
        return new MemoryCacheStore(properties);
    }

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager cacheManager(CacheStore cacheStore) {
        return new ComModuleCacheManager(cacheStore);
    }
}

