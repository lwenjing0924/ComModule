package com.commodule.cache;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MemoryCacheStore implements CacheStore {

    private final CacheProperties properties;
    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    public MemoryCacheStore(CacheProperties properties) {
        this.properties = properties;
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        if (key == null) {
            return Optional.empty();
        }
        Entry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            entries.remove(key, entry);
            return Optional.empty();
        }
        if (!type.isInstance(entry.value())) {
            throw new ClassCastException("缓存值类型不匹配: " + key);
        }
        return Optional.of(type.cast(entry.value()));
    }

    @Override
    public void put(String key, Object value) {
        put(key, value, properties.getDefaultTtl());
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("缓存 key 不能为空");
        }
        if (value == null) {
            throw new IllegalArgumentException("缓存值不能为空");
        }
        cleanupExpired();
        if (!entries.containsKey(key)) {
            ensureCapacity();
        }
        Duration effectiveTtl = ttl == null ? properties.getDefaultTtl() : ttl;
        long expiresAt = effectiveTtl.isZero() || effectiveTtl.isNegative()
                ? 0L
                : System.nanoTime() + effectiveTtl.toNanos();
        entries.put(key, new Entry(value, expiresAt));
    }

    @Override
    public boolean evict(String key) {
        return key != null && entries.remove(key) != null;
    }

    @Override
    public void evictByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return;
        }
        entries.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public void clear() {
        entries.clear();
    }

    private void ensureCapacity() {
        int maxEntries = Math.max(1, properties.getMaxEntries());
        while (entries.size() >= maxEntries) {
            Iterator<Map.Entry<String, Entry>> iterator = entries.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private void cleanupExpired() {
        entries.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private record Entry(Object value, long expiresAtNanos) {

        private boolean isExpired() {
            return expiresAtNanos > 0 && System.nanoTime() >= expiresAtNanos;
        }
    }
}

