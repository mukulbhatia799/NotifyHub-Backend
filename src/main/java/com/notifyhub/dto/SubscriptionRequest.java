package com.notifyhub.dto;

import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.NotificationLog;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRequest {

    @NotNull(message = "Event type is required")
    private NotificationEvent.EventType eventType;

    @NotNull(message = "Channel is required")
    private NotificationLog.Channel channel;
}
