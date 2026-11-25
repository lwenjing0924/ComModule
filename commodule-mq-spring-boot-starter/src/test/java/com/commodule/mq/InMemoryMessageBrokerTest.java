package com.commodule.mq;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryMessageBrokerTest {

    @Test
    void deliversMessageAsynchronously() throws Exception {
        MessageProperties properties = new MessageProperties();
        DeadLetterQueue deadLetterQueue = new InMemoryDeadLetterQueue(10);
        CountDownLatch received = new CountDownLatch(1);
        MessageListener listener = new MessageListener() {
            @Override
            public String topic() {
                return "orders";
            }

            @Override
            public void onMessage(Message message) {
                assertThat(message.payload()).isEqualTo("created");
                assertThat(message.attempt()).isEqualTo(1);
                received.countDown();
            }
        };

        try (InMemoryMessageBroker broker = new InMemoryMessageBroker(
                properties,
                deadLetterQueue,
                List.of(listener)
        )) {
            String messageId = broker.publish("orders", "created");

            assertThat(messageId).isNotBlank();
            assertThat(received.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(deadLetterQueue.list()).isEmpty();
        }
    }

    @Test
    void retriesAndMovesFailedMessageToDeadLetterQueue() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setMaxAttempts(2);
        properties.setRetryBackoff(Duration.ofMillis(1));
        DeadLetterQueue deadLetterQueue = new InMemoryDeadLetterQueue(10);
        AtomicInteger attempts = new AtomicInteger();
        MessageListener listener = new MessageListener() {
            @Override
            public String topic() {
                return "orders";
            }

            @Override
            public void onMessage(Message message) {
                attempts.incrementAndGet();
                throw new IllegalStateException("downstream unavailable");
            }
        };

        try (InMemoryMessageBroker broker = new InMemoryMessageBroker(
                properties,
                deadLetterQueue,
                List.of(listener)
        )) {
            broker.publish("orders", "created");

            for (int index = 0; index < 100 && deadLetterQueue.list().isEmpty(); index++) {
                Thread.sleep(10);
            }

            assertThat(attempts.get()).isEqualTo(2);
            assertThat(deadLetterQueue.list()).hasSize(1);
        }
    }

    @Test
    void rejectsMessageWhenQueueIsFull() throws Exception {
        MessageProperties properties = new MessageProperties();
        properties.setWorkerThreads(1);
        properties.setQueueCapacity(1);
        properties.setMaxAttempts(1);
        DeadLetterQueue deadLetterQueue = new InMemoryDeadLetterQueue(10);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        MessageListener listener = new MessageListener() {
            @Override
            public String topic() {
                return "orders";
            }

            @Override
            public void onMessage(Message message) throws Exception {
                if ("block".equals(message.payload())) {
                    entered.countDown();
                    release.await(2, TimeUnit.SECONDS);
                }
            }
        };

        try (InMemoryMessageBroker broker = new InMemoryMessageBroker(
                properties,
                deadLetterQueue,
                List.of(listener)
        )) {
            broker.publish("orders", "block");
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            broker.publish("orders", "queued");
            broker.publish("orders", "rejected");

            assertThat(deadLetterQueue.list())
                    .extracting(letter -> letter.message().payload())
                    .contains("rejected");
        } finally {
            release.countDown();
        }
    }
}

