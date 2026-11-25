package com.commodule.mq;

public interface MessageBroker extends MessagePublisher {

    void subscribe(MessageListener listener);

    void unsubscribe(MessageListener listener);
}

