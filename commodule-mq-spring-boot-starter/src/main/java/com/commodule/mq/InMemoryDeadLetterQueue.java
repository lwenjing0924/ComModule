package com.commodule.mq;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public class InMemoryDeadLetterQueue implements DeadLetterQueue {

    private final int maxSize;
    private final Deque<DeadLetter> letters = new ArrayDeque<>();

    public InMemoryDeadLetterQueue(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
    }

    @Override
    public synchronized void add(Message message, Throwable error) {
        while (letters.size() >= maxSize) {
            letters.pollFirst();
        }
        letters.addLast(new DeadLetter(message, error, Instant.now()));
    }

    @Override
    public synchronized List<DeadLetter> list() {
        return List.copyOf(letters);
    }

    @Override
    public synchronized void clear() {
        letters.clear();
    }
}

