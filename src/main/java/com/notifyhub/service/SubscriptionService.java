package com.notifyhub.service;

import com.notifyhub.dto.SubscriptionDto;
import com.notifyhub.dto.SubscriptionRequest;
import com.notifyhub.exception.ResourceNotFoundException;
import com.notifyhub.exception.ValidationException;
import com.notifyhub.model.Subscription;
import com.notifyhub.model.User;
import com.notifyhub.repository.SubscriptionRepository;
import com.notifyhub.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository         userRepository;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                                UserRepository userRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository         = userRepository;
    }

    @Transactional
    public SubscriptionDto create(SubscriptionRequest request) {
        User user = currentUser();

        if (subscriptionRepository.existsByUserIdAndEventTypeAndChannel(
                user.getId(), request.getEventType(), request.getChannel())) {
            throw new ValidationException(
                    "Subscription already exists for this event type and channel");
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .eventType(request.getEventType())
                .channel(request.getChannel())
                .active(true)
                .build();

        subscriptionRepository.save(subscription);
        log.info("Subscription created: userId={} eventType={} channel={}",
                user.getId(), request.getEventType(), request.getChannel());

        return SubscriptionDto.from(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionDto> listMine() {
        User user = currentUser();
        return subscriptionRepository.findByUserIdAndActiveTrue(user.getId())
                .stream()
                .map(SubscriptionDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deactivate(UUID id) {
        User         user = currentUser();
        Subscription sub  = subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription", "id", id));

        if (!sub.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Subscription", "id", id);
        }

        sub.setActive(false);
        subscriptionRepository.save(sub);
        log.info("Subscription deactivated: id={}", id);
    }

    public User currentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
