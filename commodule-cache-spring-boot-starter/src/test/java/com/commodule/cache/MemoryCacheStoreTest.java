package com.commodule.cache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryCacheStoreTest {

    @Test
    void storesAndEvictsValue() {
        MemoryCacheStore store = new MemoryCacheStore(new CacheProperties());

        store.put("user:1", "alice");

        assertThat(store.get("user:1", String.class)).contains("alice");
        assertThat(store.evict("user:1")).isTrue();
        assertThat(store.get("user:1", String.class)).isEmpty();
    }

    @Test
    void expiresValue() throws Exception {
        MemoryCacheStore store = new MemoryCacheStore(new CacheProperties());
        store.put("short-lived", "value", Duration.ofMillis(1));

        Thread.sleep(20);

        assertThat(store.get("short-lived", String.class)).isEmpty();
    }

    @Test
    void rejectsWrongType() {
        MemoryCacheStore store = new MemoryCacheStore(new CacheProperties());
        store.put("number", "not-a-number");

        assertThatThrownBy(() -> store.get("number", Integer.class))
                .isInstanceOf(ClassCastException.class);
    }

    @Test
    void updatingExistingKeyDoesNotEvictAnotherEntry() {
        CacheProperties properties = new CacheProperties();
        properties.setMaxEntries(2);
        MemoryCacheStore store = new MemoryCacheStore(properties);
        store.put("first", "one");
        store.put("second", "two");

        store.put("second", "updated");

        assertThat(store.get("first", String.class)).contains("one");
        assertThat(store.get("second", String.class)).contains("updated");
    }
}

