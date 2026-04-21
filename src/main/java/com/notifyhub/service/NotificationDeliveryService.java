package com.notifyhub.service;

import com.notifyhub.dto.NotificationMessageDto;
import com.notifyhub.dto.WebSocketNotificationMessage;
import com.notifyhub.exception.ResourceNotFoundException;
import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.NotificationLog;
import com.notifyhub.model.Subscription;
import com.notifyhub.repository.NotificationEventRepository;
import com.notifyhub.repository.NotificationLogRepository;
import com.notifyhub.repository.SubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class NotificationDeliveryService {

    private static final int MAX_ATTEMPTS = 3;

    private final SubscriptionRepository      subscriptionRepository;
    private final NotificationLogRepository   logRepository;
    private final NotificationEventRepository eventRepository;
    private final SimpMessagingTemplate       messagingTemplate;
    private final EmailService                emailService;

    public NotificationDeliveryService(SubscriptionRepository subscriptionRepository,
                                        NotificationLogRepository logRepository,
                                        NotificationEventRepository eventRepository,
                                        SimpMessagingTemplate messagingTemplate,
                                        EmailService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.logRepository          = logRepository;
        this.eventRepository        = eventRepository;
        this.messagingTemplate      = messagingTemplate;
        this.emailService           = emailService;
    }

    @Transactional
    public void deliver(NotificationMessageDto message) {
        NotificationEvent event = eventRepository.findById(message.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "NotificationEvent", "id", message.getEventId()));

        List<Subscription> subscriptions = subscriptionRepository
                .findByEventTypeAndActiveTrue(message.getEventType());

        if (subscriptions.isEmpty()) {
            log.info("No active subscriptions for eventType={}", message.getEventType());
            event.setStatus(NotificationEvent.EventStatus.DELIVERED);
            eventRepository.save(event);
            return;
        }

        boolean allDelivered = true;

        for (Subscription sub : subscriptions) {
            // Only deliver on requested channels
            if (!message.getChannels().contains(sub.getChannel())) {
                continue;
            }

            if (sub.getChannel() == NotificationLog.Channel.WEBSOCKET) {
                allDelivered = deliverViaWebSocket(event, sub) && allDelivered;
            } else if (sub.getChannel() == NotificationLog.Channel.EMAIL) {
                allDelivered = deliverViaEmail(event, sub) && allDelivered;
            }
        }

        event.setStatus(allDelivered
                ? NotificationEvent.EventStatus.DELIVERED
                : NotificationEvent.EventStatus.FAILED);
        eventRepository.save(event);
    }

    private boolean deliverViaWebSocket(NotificationEvent event, Subscription sub) {
        String userId    = sub.getUser().getId().toString();
        String topic     = "/topic/notifications/" + userId;

        // Check existing log for retry count
        NotificationLog log = findOrCreateLog(event, sub);

        try {
            WebSocketNotificationMessage wsMsg = WebSocketNotificationMessage.builder()
                    .eventId(event.getId())
                    .eventType(event.getEventType())
                    .payload(event.getPayload())
                    .deliveredAt(Instant.now())
                    .build();

            messagingTemplate.convertAndSend(topic, wsMsg);

            log.setDeliveryStatus(NotificationLog.DeliveryStatus.SUCCESS);
            log.setLastAttemptAt(Instant.now());
            logRepository.save(log);

            return true;

        } catch (Exception e) {
            int attempts = log.getAttemptCount() + 1;
            log.setAttemptCount(attempts);
            log.setLastAttemptAt(Instant.now());
            log.setErrorMessage(e.getMessage());

            if (attempts >= MAX_ATTEMPTS) {
                log.setDeliveryStatus(NotificationLog.DeliveryStatus.FAILED);
                logRepository.save(log);
                return false;
            }

            log.setDeliveryStatus(NotificationLog.DeliveryStatus.RETRYING);
            logRepository.save(log);
            // Caller (consumer) will route to DLQ after MAX_ATTEMPTS
            throw new RuntimeException("WebSocket delivery failed, attempt " + attempts, e);
        }
    }

    private boolean deliverViaEmail(NotificationEvent event, Subscription sub) {
        String toEmail = sub.getUser().getEmail();
        NotificationLog log = findOrCreateLog(event, sub);

        try {
            emailService.sendNotification(toEmail, event);

            log.setDeliveryStatus(NotificationLog.DeliveryStatus.SUCCESS);
            log.setLastAttemptAt(Instant.now());
            logRepository.save(log);

            return true;

        } catch (Exception e) {
            int attempts = log.getAttemptCount() + 1;
            log.setAttemptCount(attempts);
            log.setLastAttemptAt(Instant.now());
            log.setErrorMessage(e.getMessage());

            if (attempts >= MAX_ATTEMPTS) {
                log.setDeliveryStatus(NotificationLog.DeliveryStatus.FAILED);
                logRepository.save(log);
                return false;
            }

            log.setDeliveryStatus(NotificationLog.DeliveryStatus.RETRYING);
            logRepository.save(log);
            throw new RuntimeException("Email delivery failed, attempt " + attempts, e);
        }
    }

    private NotificationLog findOrCreateLog(NotificationEvent event, Subscription sub) {
        return logRepository.findByEventId(event.getId())
                .stream()
                .filter(l -> l.getChannel() == sub.getChannel())
                .findFirst()
                .orElseGet(() -> NotificationLog.builder()
                        .event(event)
                        .channel(sub.getChannel())
                        .deliveryStatus(NotificationLog.DeliveryStatus.RETRYING)
                        .attemptCount(0)
                        .build());
    }
}
