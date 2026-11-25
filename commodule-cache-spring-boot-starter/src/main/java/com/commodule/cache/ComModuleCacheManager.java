package com.commodule.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ComModuleCacheManager implements CacheManager {

    private final CacheStore cacheStore;
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();

    public ComModuleCacheManager(CacheStore cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Override
    public Cache getCache(String name) {
        return caches.computeIfAbsent(name, cacheName -> new ComModuleCache(cacheName, cacheStore));
    }

    @Override
    public Collection<String> getCacheNames() {
        return Set.copyOf(caches.keySet());
    }
}

