package com.notifyhub.dto;

import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.NotificationLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO serialised to JSON and sent over RabbitMQ.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessageDto {

    private UUID                        eventId;
    private NotificationEvent.EventType eventType;
    private String                      payload;
    private String                      tenantId;
    private List<NotificationLog.Channel> channels;
}
