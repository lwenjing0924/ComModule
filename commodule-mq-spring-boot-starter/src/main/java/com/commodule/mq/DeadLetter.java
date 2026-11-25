package com.commodule.mq;

import java.time.Instant;

public record DeadLetter(Message message, Throwable error, Instant failedAt) {
}

