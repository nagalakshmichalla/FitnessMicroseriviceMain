

package com.fitness.activityservice.config;

import org.springframework.amqp.core.*;
        import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "fitness.exchange";
    public static final String QUEUE = "activity.queue";
    public static final String ROUTING_KEY = "activity.tracking";

    @Bean
    public TopicExchange fitnessExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue activityQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding activityBinding(Queue activityQueue, TopicExchange fitnessExchange) {
        return BindingBuilder
                .bind(activityQueue)
                .to(fitnessExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

