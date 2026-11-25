package com.commodule.cache;

import java.time.Duration;
import java.util.Optional;

public interface CacheStore {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value);

    void put(String key, Object value, Duration ttl);

    boolean evict(String key);

    void evictByPrefix(String prefix);

    void clear();
}

