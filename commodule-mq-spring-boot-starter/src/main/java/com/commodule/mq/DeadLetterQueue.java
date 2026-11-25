package com.commodule.mq;

import java.util.List;

public interface DeadLetterQueue {

    void add(Message message, Throwable error);

    List<DeadLetter> list();

    void clear();
}

