package com.commodule.mq;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDeadLetterQueueTest {

    @Test
    void keepsOnlyConfiguredCapacity() {
        InMemoryDeadLetterQueue queue = new InMemoryDeadLetterQueue(1);

        queue.add(message("first"), new RuntimeException("first"));
        queue.add(message("second"), new RuntimeException("second"));

        assertThat(queue.list()).hasSize(1);
        assertThat(queue.list().get(0).message().payload()).isEqualTo("second");
    }

    private Message message(String payload) {
        return new Message("id-" + payload, "orders", payload, Map.of(), Instant.now(), 1);
    }
}

