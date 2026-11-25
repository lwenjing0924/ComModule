package com.commodule.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRefreshTokenStoreTest {

    @Test
    void consumesTokenOnlyOnce() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore(Duration.ofMinutes(5), 10);
        IssuedRefreshToken issued = store.issue(1L, "alice");

        Optional<RefreshTokenRecord> consumed = store.consume(issued.token());

        assertThat(consumed).isPresent();
        assertThat(consumed.orElseThrow().userId()).isEqualTo(1L);
        assertThat(store.consume(issued.token())).isEmpty();
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore(Duration.ofMillis(5), 10);
        IssuedRefreshToken issued = store.issue(1L, "alice");

        Thread.sleep(20);

        assertThat(store.consume(issued.token())).isEmpty();
    }

    @Test
    void revokesAllTokensForUser() {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore(Duration.ofMinutes(5), 10);
        IssuedRefreshToken first = store.issue(1L, "alice");
        IssuedRefreshToken second = store.issue(1L, "alice");
        IssuedRefreshToken other = store.issue(2L, "bob");

        store.revokeAll(1L);

        assertThat(store.consume(first.token())).isEmpty();
        assertThat(store.consume(second.token())).isEmpty();
        assertThat(store.consume(other.token())).isPresent();
    }

    @Test
    void dropsOldestTokenWhenUserLimitIsReached() throws Exception {
        InMemoryRefreshTokenStore store = new InMemoryRefreshTokenStore(Duration.ofMinutes(5), 2);
        IssuedRefreshToken first = store.issue(1L, "alice");
        Thread.sleep(5);
        IssuedRefreshToken second = store.issue(1L, "alice");
        Thread.sleep(5);
        IssuedRefreshToken third = store.issue(1L, "alice");

        assertThat(store.consume(first.token())).isEmpty();
        assertThat(store.consume(second.token())).isPresent();
        assertThat(store.consume(third.token())).isPresent();
    }
}
