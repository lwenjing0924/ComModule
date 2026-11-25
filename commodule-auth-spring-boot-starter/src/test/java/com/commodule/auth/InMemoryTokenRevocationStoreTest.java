package com.commodule.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryTokenRevocationStoreTest {

    @Test
    void revokesTokenUntilExpiration() {
        InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();

        store.revoke("token-1", Instant.now().plusSeconds(60));

        assertThat(store.isRevoked("token-1")).isTrue();
        assertThat(store.isRevoked("token-2")).isFalse();
    }

    @Test
    void ignoresExpiredRevocation() {
        InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();

        store.revoke("token-1", Instant.now().minusSeconds(1));

        assertThat(store.isRevoked("token-1")).isFalse();
    }

    @Test
    void removesRevocationAfterExpiration() throws Exception {
        InMemoryTokenRevocationStore store = new InMemoryTokenRevocationStore();
        store.revoke("token-1", Instant.now().plusMillis(5));

        Thread.sleep(20);

        assertThat(store.isRevoked("token-1")).isFalse();
    }
}
