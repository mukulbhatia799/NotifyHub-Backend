package com.notifyhub.service;

import com.notifyhub.dto.NotificationEventDto;
import com.notifyhub.dto.NotificationMessageDto;
import com.notifyhub.dto.PublishEventRequest;
import com.notifyhub.dto.PublishEventResponse;
import com.notifyhub.exception.ResourceNotFoundException;
import com.notifyhub.messaging.NotificationProducer;
import com.notifyhub.model.NotificationEvent;
import com.notifyhub.repository.NotificationEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

@Service
@Slf4j
public class EventService {

    private final NotificationEventRepository eventRepository;
    private final NotificationProducer        producer;

    public EventService(NotificationEventRepository eventRepository,
                        NotificationProducer producer) {
        this.eventRepository = eventRepository;
        this.producer        = producer;
    }

    @Transactional
    public PublishEventResponse publish(PublishEventRequest request) {
        // 1. Persist with PENDING status
        NotificationEvent event = NotificationEvent.builder()
                .eventType(request.getEventType())
                .payload(request.getPayload())
                .tenantId(request.getTenantId())
                .status(NotificationEvent.EventStatus.PENDING)
                .build();
        eventRepository.save(event);
        log.info("Event saved with id={} status=PENDING", event.getId());

        // 2. Update status to QUEUED
        event.setStatus(NotificationEvent.EventStatus.QUEUED);
        eventRepository.save(event);
        log.info("Event id={} status=QUEUED", event.getId());

        // 3. Publish to RabbitMQ AFTER transaction commits to avoid race condition
        //    where consumer reads DB before our transaction is visible
        NotificationMessageDto message = NotificationMessageDto.builder()
                .eventId(event.getId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .tenantId(event.getTenantId())
                .channels(request.getChannels())
                .build();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                producer.publish(message);
            }
        });

        return PublishEventResponse.builder()
                .eventId(event.getId())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .message("Event published successfully")
                .build();
    }

    @Transactional(readOnly = true)
    public Page<NotificationEventDto> listEvents(NotificationEvent.EventStatus status,
                                                  NotificationEvent.EventType eventType,
                                                  Pageable pageable) {
        Page<NotificationEvent> page;

        if (status != null && eventType != null) {
            page = eventRepository.findByStatusAndEventType(status, eventType, pageable);
        } else if (status != null) {
            page = eventRepository.findByStatus(status, pageable);
        } else if (eventType != null) {
            page = eventRepository.findByEventType(eventType, pageable);
        } else {
            page = eventRepository.findAll(pageable);
        }

        return page.map(NotificationEventDto::from);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "event", key = "#id")
    public NotificationEventDto getEventById(UUID id) {
        NotificationEvent event = eventRepository.findByIdWithLogs(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationEvent", "id", id));
        return NotificationEventDto.fromWithLogs(event);
    }

    @Transactional
    @CacheEvict(value = "event", key = "#id")
    public void updateEventStatus(UUID id, NotificationEvent.EventStatus status) {
        NotificationEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("NotificationEvent", "id", id));
        event.setStatus(status);
        eventRepository.save(event);
    }
}
