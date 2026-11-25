package com.commodule.mq;

public interface MessageListener {

    String topic();

    void onMessage(Message message) throws Exception;
}

