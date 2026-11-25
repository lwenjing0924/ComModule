package com.commodule.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ComModuleCacheManagerTest {

    @Test
    void adaptsStoreToSpringCache() {
        CacheProperties properties = new CacheProperties();
        CacheStore store = new MemoryCacheStore(properties);
        ComModuleCacheManager cacheManager = new ComModuleCacheManager(store);
        Cache cache = cacheManager.getCache("products");

        cache.put(1L, "keyboard");

        assertThat(cache.get(1L).get()).isEqualTo("keyboard");
        assertThat(cacheManager.getCacheNames()).contains("products");
    }

    @Test
    void loaderIsCalledOnce() throws Exception {
        ComModuleCacheManager cacheManager =
                new ComModuleCacheManager(new MemoryCacheStore(new CacheProperties()));
        Cache cache = cacheManager.getCache("loader");
        AtomicInteger calls = new AtomicInteger();

        String first = cache.get("key", () -> "value-" + calls.incrementAndGet());
        String second = cache.get("key", () -> "value-" + calls.incrementAndGet());

        assertThat(first).isEqualTo("value-1");
        assertThat(second).isEqualTo("value-1");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void clearsOnlyTheSelectedCache() {
        ComModuleCacheManager cacheManager =
                new ComModuleCacheManager(new MemoryCacheStore(new CacheProperties()));
        Cache products = cacheManager.getCache("products");
        Cache users = cacheManager.getCache("users");
        products.put("1", "keyboard");
        users.put("1", "alice");

        products.clear();

        assertThat(products.get("1")).isNull();
        assertThat(users.get("1").get()).isEqualTo("alice");
    }

    @Test
    void concurrentLoaderRunsOnlyOnce() throws Exception {
        Cache cache = new ComModuleCacheManager(new MemoryCacheStore(new CacheProperties()))
                .getCache("hot-key");
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < 16; index++) {
                futures.add(executor.submit(() -> {
                    start.await(2, TimeUnit.SECONDS);
                    return cache.get("same-key", () -> {
                        loads.incrementAndGet();
                        Thread.sleep(30);
                        return "loaded";
                    });
                }));
            }
            start.countDown();
            for (Future<String> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("loaded");
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(loads.get()).isEqualTo(1);
    }
}

