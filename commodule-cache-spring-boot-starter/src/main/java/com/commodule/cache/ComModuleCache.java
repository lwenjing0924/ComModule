package com.commodule.cache;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

public class ComModuleCache implements Cache {

    private static final int LOAD_LOCK_COUNT = 64;

    private final String name;
    private final CacheStore cacheStore;
    private final ReentrantLock[] loadLocks;

    public ComModuleCache(String name, CacheStore cacheStore) {
        this.name = name;
        this.cacheStore = cacheStore;
        this.loadLocks = new ReentrantLock[LOAD_LOCK_COUNT];
        for (int index = 0; index < loadLocks.length; index++) {
            loadLocks[index] = new ReentrantLock();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return cacheStore;
    }

    @Override
    public ValueWrapper get(Object key) {
        return cacheStore.get(cacheKey(key), Object.class)
                .map(SimpleValueWrapper::new)
                .orElse(null);
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return cacheStore.get(cacheKey(key), type).orElse(null);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        String resolvedKey = cacheKey(key);
        T cached = cast(readValue(resolvedKey));
        if (cached != null) {
            return cached;
        }
        ReentrantLock loadLock = lockFor(resolvedKey);
        loadLock.lock();
        try {
            cached = cast(readValue(resolvedKey));
            if (cached != null) {
                return cached;
            }
            try {
                T value = valueLoader.call();
                if (value != null) {
                    cacheStore.put(resolvedKey, value);
                }
                return value;
            } catch (Exception exception) {
                throw new Cache.ValueRetrievalException(key, valueLoader, exception);
            }
        } finally {
            loadLock.unlock();
        }
    }

    @Override
    public void put(Object key, Object value) {
        cacheStore.put(cacheKey(key), value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        String resolvedKey = cacheKey(key);
        ReentrantLock loadLock = lockFor(resolvedKey);
        loadLock.lock();
        try {
            ValueWrapper existing = get(key);
            if (existing != null) {
                return existing;
            }
            cacheStore.put(resolvedKey, value);
            return null;
        } finally {
            loadLock.unlock();
        }
    }

    @Override
    public void evict(Object key) {
        cacheStore.evict(cacheKey(key));
    }

    @Override
    public boolean evictIfPresent(Object key) {
        return cacheStore.evict(cacheKey(key));
    }

    @Override
    public void clear() {
        cacheStore.evictByPrefix(name + "::");
    }

    private String cacheKey(Object key) {
        return name + "::" + key;
    }

    private Object readValue(String resolvedKey) {
        return cacheStore.get(resolvedKey, Object.class).orElse(null);
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        return (T) value;
    }

    private ReentrantLock lockFor(String resolvedKey) {
        return loadLocks[Math.floorMod(resolvedKey.hashCode(), loadLocks.length)];
    }
}

