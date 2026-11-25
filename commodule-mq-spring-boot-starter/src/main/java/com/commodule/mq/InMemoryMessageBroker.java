package com.commodule.mq;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryMessageBroker implements MessageBroker, AutoCloseable {

    private final MessageProperties properties;
    private final DeadLetterQueue deadLetterQueue;
    private final ConcurrentMap<String, CopyOnWriteArrayList<MessageListener>> listeners = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor executorService;

    public InMemoryMessageBroker(
            MessageProperties properties,
            DeadLetterQueue deadLetterQueue,
            List<MessageListener> initialListeners
    ) {
        this.properties = properties;
        this.deadLetterQueue = deadLetterQueue;
        AtomicInteger threadIndex = new AtomicInteger();
        int workerThreads = Math.max(1, properties.getWorkerThreads());
        this.executorService = new ThreadPoolExecutor(
                workerThreads,
                workerThreads,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(Math.max(1, properties.getQueueCapacity())),
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName("commodule-mq-" + threadIndex.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        initialListeners.forEach(this::subscribe);
    }

    @Override
    public String publish(String topic, String payload) {
        return publish(topic, payload, Map.of());
    }

    @Override
    public String publish(String topic, String payload, Map<String, String> headers) {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("消息 topic 不能为空");
        }
        Message message = new Message(
                UUID.randomUUID().toString().replace("-", ""),
                topic,
                payload,
                headers,
                Instant.now(),
                0
        );
        boolean rejected = false;
        for (MessageListener listener : listeners.getOrDefault(topic, new CopyOnWriteArrayList<>())) {
            if (rejected) {
                break;
            }
            try {
                executorService.submit(() -> dispatch(listener, message));
            } catch (RejectedExecutionException exception) {
                rejected = true;
                deadLetterQueue.add(message, exception);
            }
        }
        return message.id();
    }

    @Override
    public void subscribe(MessageListener listener) {
        if (listener == null || listener.topic() == null || listener.topic().isBlank()) {
            throw new IllegalArgumentException("监听器及其 topic 不能为空");
        }
        CopyOnWriteArrayList<MessageListener> topicListeners =
                listeners.computeIfAbsent(listener.topic(), ignored -> new CopyOnWriteArrayList<>());
        if (!topicListeners.contains(listener)) {
            topicListeners.add(listener);
        }
    }

    @Override
    public void unsubscribe(MessageListener listener) {
        if (listener == null) {
            return;
        }
        CopyOnWriteArrayList<MessageListener> topicListeners = listeners.get(listener.topic());
        if (topicListeners == null) {
            return;
        }
        topicListeners.remove(listener);
        if (topicListeners.isEmpty()) {
            listeners.remove(listener.topic(), topicListeners);
        }
    }

    private void dispatch(MessageListener listener, Message message) {
        int maxAttempts = Math.max(1, properties.getMaxAttempts());
        Throwable lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                listener.onMessage(message.withAttempt(attempt));
                return;
            } catch (Exception exception) {
                lastError = exception;
                if (attempt < maxAttempts && !sleepBeforeRetry()) {
                    break;
                }
            }
        }
        deadLetterQueue.add(message, lastError);
    }

    private boolean sleepBeforeRetry() {
        long backoffMillis = Math.max(0, properties.getRetryBackoff().toMillis());
        if (backoffMillis == 0) {
            return true;
        }
        try {
            TimeUnit.MILLISECONDS.sleep(backoffMillis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }
}

