package com.commodule.mq;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MessageAutoConfigurationTest {

    @Test
    void providesDefaultBrokerAndDeadLetterQueue() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MessageAutoConfiguration.class))
                .run(context -> {
                    assertThat(context.getBean(MessageBroker.class)).isInstanceOf(InMemoryMessageBroker.class);
                    assertThat(context.getBean(DeadLetterQueue.class)).isInstanceOf(InMemoryDeadLetterQueue.class);
                });
    }
}

