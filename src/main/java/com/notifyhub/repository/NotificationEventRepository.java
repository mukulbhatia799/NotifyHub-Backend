package com.notifyhub.repository;

import com.notifyhub.model.NotificationEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {

    Page<NotificationEvent> findByStatus(NotificationEvent.EventStatus status, Pageable pageable);

    Page<NotificationEvent> findByEventType(NotificationEvent.EventType eventType, Pageable pageable);

    Page<NotificationEvent> findByStatusAndEventType(
            NotificationEvent.EventStatus status,
            NotificationEvent.EventType eventType,
            Pageable pageable);

    @EntityGraph(attributePaths = "logs")
    @Query("SELECT e FROM NotificationEvent e WHERE e.id = :id")
    Optional<NotificationEvent> findByIdWithLogs(UUID id);
}
