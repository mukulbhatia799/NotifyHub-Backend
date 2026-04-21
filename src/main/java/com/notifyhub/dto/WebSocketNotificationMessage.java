package com.notifyhub.dto;

import com.notifyhub.model.NotificationEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketNotificationMessage {

    private UUID                        eventId;
    private NotificationEvent.EventType eventType;
    private String                      payload;
    private Instant                     deliveredAt;
}
