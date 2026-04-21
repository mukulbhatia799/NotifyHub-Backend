package com.notifyhub.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.queue}")
    private String queue;

    @Value("${app.rabbitmq.dlq}")
    private String dlq;

    @Value("${app.rabbitmq.routing-key-prefix}")
    private String routingKeyPrefix;

    @Value("${app.rabbitmq.dlq-routing-key}")
    private String dlqRoutingKey;

    @Value("${app.rabbitmq.message-ttl}")
    private long messageTtl;

    // ------------------------------------------------------------------ //
    //  Exchange
    // ------------------------------------------------------------------ //

    @Bean
    public TopicExchange notificationExchange() {
        return ExchangeBuilder.topicExchange(exchange)
                .durable(true)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Queues
    // ------------------------------------------------------------------ //

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(queue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(dlq)
                .withArgument("x-message-ttl", messageTtl)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Bindings
    // ------------------------------------------------------------------ //

    @Bean
    public Binding notificationBinding(Queue notificationQueue,
                                       TopicExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(routingKeyPrefix + ".#");
    }

    @Bean
    public Binding dlqBinding(Queue deadLetterQueue,
                              TopicExchange notificationExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(notificationExchange)
                .with(dlqRoutingKey);
    }

    // ------------------------------------------------------------------ //
    //  Message converter & template
    // ------------------------------------------------------------------ //

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }
}
