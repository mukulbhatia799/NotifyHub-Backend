package com.notifyhub;

import com.notifyhub.dto.NotificationMessageDto;
import com.notifyhub.messaging.NotificationConsumer;
import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.NotificationLog;
import com.notifyhub.service.NotificationDeliveryService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationConsumer — consumer logic with retry/DLQ routing")
class NotificationConsumerTest {

    @Mock NotificationDeliveryService deliveryService;
    @Mock RabbitTemplate              rabbitTemplate;
    @Mock Channel                     channel;

    private NotificationConsumer consumer;

    private static final String EXCHANGE    = "notification.exchange";
    private static final String DLQ_ROUTING = "notification.failed";

    @BeforeEach
    void setUp() {
        consumer = new NotificationConsumer(deliveryService, rabbitTemplate, EXCHANGE, DLQ_ROUTING);
    }

    private NotificationMessageDto buildMessage() {
        return NotificationMessageDto.builder()
                .eventId(UUID.randomUUID())
                .eventType(NotificationEvent.EventType.ORDER_PLACED)
                .payload("{\"orderId\":\"abc\"}")
                .tenantId("tenant-1")
                .channels(List.of(NotificationLog.Channel.WEBSOCKET))
                .build();
    }

    private Message rawMessage(int deliveryCount) {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        props.getHeaders().put("x-delivery-count", deliveryCount);
        return new Message(new byte[0], props);
    }

    @Test
    @DisplayName("Successful delivery acks the message")
    void consume_success_acksMessage() throws IOException {
        NotificationMessageDto msg = buildMessage();
        doNothing().when(deliveryService).deliver(msg);

        consumer.consume(msg, rawMessage(0), channel);

        verify(channel).basicAck(1L, false);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
    }

    @Test
    @DisplayName("First failure requeues (attempt < MAX)")
    void consume_firstFailure_requeues() throws IOException {
        NotificationMessageDto msg = buildMessage();
        doThrow(new RuntimeException("ws error")).when(deliveryService).deliver(msg);

        consumer.consume(msg, rawMessage(0), channel);

        verify(channel).basicNack(1L, false, true);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
    }

    @Test
    @DisplayName("Third failure routes to DLQ and acks")
    void consume_thirdFailure_routesToDlq() throws IOException {
        NotificationMessageDto msg = buildMessage();
        doThrow(new RuntimeException("ws error")).when(deliveryService).deliver(msg);

        // Simulate delivery count = 2 (so attempt = 3 = MAX)
        consumer.consume(msg, rawMessage(2), channel);

        verify(rabbitTemplate).convertAndSend(EXCHANGE, DLQ_ROUTING, msg);
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }
}
