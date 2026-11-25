package com.commodule.mq;

import java.time.Instant;
import java.util.Map;

public record Message(
        String id,
        String topic,
        String payload,
        Map<String, String> headers,
        Instant createdAt,
        int attempt
) {

    public Message {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }

    public Message withAttempt(int nextAttempt) {
        return new Message(id, topic, payload, headers, createdAt, nextAttempt);
    }
}

