package com.notifyhub.controller;

import com.notifyhub.dto.SubscriptionDto;
import com.notifyhub.dto.SubscriptionRequest;
import com.notifyhub.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public ResponseEntity<SubscriptionDto> create(
            @Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionDto>> listMine() {
        return ResponseEntity.ok(subscriptionService.listMine());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        subscriptionService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
