package com.commodule.mq;

import java.util.Map;

public interface MessagePublisher {

    String publish(String topic, String payload);

    String publish(String topic, String payload, Map<String, String> headers);
}

