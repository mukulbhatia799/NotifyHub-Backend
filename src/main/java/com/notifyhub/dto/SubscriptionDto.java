package com.notifyhub.dto;

import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.NotificationLog;
import com.notifyhub.model.Subscription;
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
public class SubscriptionDto {

    private UUID                        id;
    private NotificationEvent.EventType eventType;
    private NotificationLog.Channel     channel;
    private boolean                     active;
    private Instant                     createdAt;

    public static SubscriptionDto from(Subscription subscription) {
        return SubscriptionDto.builder()
                .id(subscription.getId())
                .eventType(subscription.getEventType())
                .channel(subscription.getChannel())
                .active(subscription.isActive())
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}
