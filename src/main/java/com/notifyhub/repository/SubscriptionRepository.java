package com.notifyhub.repository;

import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    List<Subscription> findByUserIdAndActiveTrue(UUID userId);

    List<Subscription> findByEventTypeAndActiveTrue(NotificationEvent.EventType eventType);

    Optional<Subscription> findByUserIdAndEventTypeAndChannel(
            UUID userId,
            NotificationEvent.EventType eventType,
            com.notifyhub.model.NotificationLog.Channel channel);

    boolean existsByUserIdAndEventTypeAndChannel(
            UUID userId,
            NotificationEvent.EventType eventType,
            com.notifyhub.model.NotificationLog.Channel channel);
}
