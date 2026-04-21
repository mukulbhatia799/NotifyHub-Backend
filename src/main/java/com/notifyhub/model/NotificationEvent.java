package com.notifyhub.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notification_events",
        indexes = {
                @Index(name = "idx_ne_status",     columnList = "status"),
                @Index(name = "idx_ne_event_type", columnList = "event_type"),
                @Index(name = "idx_ne_tenant_id",  columnList = "tenant_id"),
                @Index(name = "idx_ne_created_at", columnList = "created_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.PENDING;

    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<NotificationLog> logs = new ArrayList<>();

    public enum EventType {
        ORDER_PLACED, PAYMENT_FAILED, USER_SIGNUP, CUSTOM
    }

    public enum EventStatus {
        PENDING, QUEUED, DELIVERED, FAILED
    }
}
