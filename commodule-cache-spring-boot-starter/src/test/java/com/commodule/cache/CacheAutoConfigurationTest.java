package com.commodule.cache;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CacheAutoConfigurationTest {

    @Test
    void providesDefaultStoreAndManager() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .run(context -> {
                    assertThat(context.getBean(CacheStore.class)).isInstanceOf(MemoryCacheStore.class);
                    assertThat(context.getBean(CacheManager.class)).isInstanceOf(ComModuleCacheManager.class);
                });
    }

    @Test
    void customStoreOverridesDefault() {
        CacheStore customStore = new CacheStore() {
            @Override
            public <T> Optional<T> get(String key, Class<T> type) {
                return Optional.empty();
            }

            @Override
            public void put(String key, Object value) {
            }

            @Override
            public void put(String key, Object value, Duration ttl) {
            }

            @Override
            public boolean evict(String key) {
                return false;
            }

            @Override
            public void evictByPrefix(String prefix) {
            }

            @Override
            public void clear() {
            }
        };

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withBean(CacheStore.class, () -> customStore)
                .run(context -> assertThat(context.getBean(CacheStore.class)).isSameAs(customStore));
    }
}

