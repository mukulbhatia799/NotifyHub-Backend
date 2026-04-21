package com.notifyhub.controller;

import com.notifyhub.dto.NotificationEventDto;
import com.notifyhub.dto.PublishEventRequest;
import com.notifyhub.dto.PublishEventResponse;
import com.notifyhub.model.NotificationEvent;
import com.notifyhub.service.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/publish")
    public ResponseEntity<PublishEventResponse> publish(
            @Valid @RequestBody PublishEventRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(eventService.publish(request));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationEventDto>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false)    NotificationEvent.EventStatus status,
            @RequestParam(required = false)    NotificationEvent.EventType   eventType) {

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(eventService.listEvents(status, eventType, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationEventDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
}
