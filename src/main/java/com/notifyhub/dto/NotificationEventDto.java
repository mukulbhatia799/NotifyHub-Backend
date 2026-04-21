package com.notifyhub.dto;

import com.notifyhub.model.NotificationEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEventDto {

    private UUID                          id;
    private NotificationEvent.EventType   eventType;
    private String                        payload;
    private NotificationEvent.EventStatus status;
    private String                        tenantId;
    private Instant                       createdAt;
    private Instant                       updatedAt;
    private List<NotificationLogDto>      logs;

    public static NotificationEventDto from(NotificationEvent event) {
        return NotificationEventDto.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .status(event.getStatus())
                .tenantId(event.getTenantId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }

    public static NotificationEventDto fromWithLogs(NotificationEvent event) {
        return NotificationEventDto.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .status(event.getStatus())
                .tenantId(event.getTenantId())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .logs(event.getLogs().stream()
                        .map(NotificationLogDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
