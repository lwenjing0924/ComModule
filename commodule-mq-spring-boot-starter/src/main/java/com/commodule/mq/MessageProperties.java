package com.commodule.mq;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "commodule.mq")
public class MessageProperties {

    private String provider = "memory";
    private int maxAttempts = 3;
    private Duration retryBackoff = Duration.ofMillis(100);
    private int maxDeadLetters = 1_000;
    private int workerThreads = 2;
    private int queueCapacity = 1_000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff;
    }

    public int getMaxDeadLetters() {
        return maxDeadLetters;
    }

    public void setMaxDeadLetters(int maxDeadLetters) {
        this.maxDeadLetters = maxDeadLetters;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }
}

