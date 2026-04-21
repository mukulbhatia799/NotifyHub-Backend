package com.notifyhub.dto;

import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.NotificationLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublishEventRequest {

    @NotNull(message = "Event type is required")
    private NotificationEvent.EventType eventType;

    @NotBlank(message = "Payload is required")
    private String payload;

    @NotBlank(message = "Tenant ID is required")
    private String tenantId;

    @NotEmpty(message = "At least one channel must be specified")
    private List<NotificationLog.Channel> channels;
}
