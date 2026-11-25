package com.commodule.template.messaging;

import com.commodule.mq.Message;
import com.commodule.mq.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class DemoOrderListener implements MessageListener {

    private final ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();

    @Override
    public String topic() {
        return "demo.orders";
    }

    @Override
    public void onMessage(Message message) {
        messages.add(message);
    }

    public List<Message> received() {
        return List.copyOf(messages);
    }
}

