package com.commodule.mq;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(MessageProperties.class)
public class MessageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DeadLetterQueue.class)
    public DeadLetterQueue inMemoryDeadLetterQueue(MessageProperties properties) {
        return new InMemoryDeadLetterQueue(properties.getMaxDeadLetters());
    }

    @Bean
    @ConditionalOnMissingBean(MessageBroker.class)
    @ConditionalOnProperty(
            prefix = "commodule.mq",
            name = "provider",
            havingValue = "memory",
            matchIfMissing = true
    )
    public MessageBroker inMemoryMessageBroker(
            MessageProperties properties,
            DeadLetterQueue deadLetterQueue,
            List<MessageListener> messageListeners
    ) {
        return new InMemoryMessageBroker(properties, deadLetterQueue, messageListeners);
    }
}

