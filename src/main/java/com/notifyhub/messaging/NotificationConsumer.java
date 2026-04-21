package com.notifyhub.messaging;

import com.notifyhub.dto.NotificationMessageDto;
import com.notifyhub.service.NotificationDeliveryService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class NotificationConsumer {

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationDeliveryService deliveryService;
    private final RabbitTemplate              rabbitTemplate;
    private final String                      exchange;
    private final String                      dlqRoutingKey;

    public NotificationConsumer(NotificationDeliveryService deliveryService,
                                 RabbitTemplate rabbitTemplate,
                                 @Value("${app.rabbitmq.exchange}") String exchange,
                                 @Value("${app.rabbitmq.dlq-routing-key}") String dlqRoutingKey) {
        this.deliveryService = deliveryService;
        this.rabbitTemplate  = rabbitTemplate;
        this.exchange        = exchange;
        this.dlqRoutingKey   = dlqRoutingKey;
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}",
            containerFactory = "rabbitListenerContainerFactory")
    public void consume(NotificationMessageDto message,
                        Message rawMessage,
                        Channel channel) throws IOException {

        long deliveryTag = rawMessage.getMessageProperties().getDeliveryTag();
        int  attempt     = getAttemptCount(rawMessage);

        log.info("Consuming event id={} attempt={}", message.getEventId(), attempt);

        try {
            deliveryService.deliver(message);
            channel.basicAck(deliveryTag, false);
            log.info("Event id={} processed successfully", message.getEventId());

        } catch (Exception e) {
            log.warn("Event id={} delivery failed (attempt {}): {}",
                    message.getEventId(), attempt, e.getMessage());

            if (attempt < MAX_ATTEMPTS) {
                // Requeue with a delay by negative-acking without requeue,
                // relying on DLQ TTL to act as the delay mechanism
                channel.basicNack(deliveryTag, false, true);
            } else {
                log.error("Event id={} exceeded max attempts, routing to DLQ", message.getEventId());
                // Route manually to DLQ
                rabbitTemplate.convertAndSend(exchange, dlqRoutingKey, message);
                channel.basicAck(deliveryTag, false);
            }
        }
    }

    private int getAttemptCount(Message message) {
        Object count = message.getMessageProperties().getHeaders()
                .get("x-delivery-count");
        if (count instanceof Number n) {
            return n.intValue() + 1;
        }
        return 1;
    }
}
