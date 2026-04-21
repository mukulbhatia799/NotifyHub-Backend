package com.notifyhub.dto;

import com.notifyhub.model.NotificationLog;
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
public class NotificationLogDto {

    private UUID                          id;
    private NotificationLog.Channel       channel;
    private NotificationLog.DeliveryStatus deliveryStatus;
    private int                           attemptCount;
    private Instant                       lastAttemptAt;
    private String                        errorMessage;
    private Instant                       createdAt;

    public static NotificationLogDto from(NotificationLog log) {
        return NotificationLogDto.builder()
                .id(log.getId())
                .channel(log.getChannel())
                .deliveryStatus(log.getDeliveryStatus())
                .attemptCount(log.getAttemptCount())
                .lastAttemptAt(log.getLastAttemptAt())
                .errorMessage(log.getErrorMessage())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
