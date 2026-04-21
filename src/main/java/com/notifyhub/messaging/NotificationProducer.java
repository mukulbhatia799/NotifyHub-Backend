package com.notifyhub.messaging;

import com.notifyhub.dto.NotificationMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationProducer {

    private final RabbitTemplate rabbitTemplate;
    private final String         exchange;
    private final String         routingKeyPrefix;

    public NotificationProducer(RabbitTemplate rabbitTemplate,
                                 @Value("${app.rabbitmq.exchange}") String exchange,
                                 @Value("${app.rabbitmq.routing-key-prefix}") String routingKeyPrefix) {
        this.rabbitTemplate   = rabbitTemplate;
        this.exchange         = exchange;
        this.routingKeyPrefix = routingKeyPrefix;
    }

    /**
     * Publishes a notification message to the topic exchange.
     * Routing key format: notification.{EVENT_TYPE}
     */
    public void publish(NotificationMessageDto message) {
        String routingKey = routingKeyPrefix + "." + message.getEventType().name().toLowerCase();
        log.info("Publishing event id={} to exchange={} routingKey={}",
                message.getEventId(), exchange, routingKey);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
