package com.notifyhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.controller.EventController;
import com.notifyhub.dto.PublishEventRequest;
import com.notifyhub.dto.PublishEventResponse;
import com.notifyhub.model.NotificationEvent;
import com.notifyhub.model.NotificationLog;
import com.notifyhub.ratelimit.RateLimitInterceptor;
import com.notifyhub.security.JwtAuthFilter;
import com.notifyhub.security.JwtUtil;
import com.notifyhub.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@DisplayName("EventController — publish endpoint")
class EventPublishingTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean EventService        eventService;
    @MockBean JwtUtil             jwtUtil;
    @MockBean JwtAuthFilter       jwtAuthFilter;
    @MockBean RateLimitInterceptor rateLimitInterceptor;
    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("POST /api/events/publish → 202 with eventId and QUEUED status")
    void publish_validRequest_returns202() throws Exception {
        UUID              eventId  = UUID.randomUUID();
        PublishEventResponse resp = PublishEventResponse.builder()
                .eventId(eventId)
                .status(NotificationEvent.EventStatus.QUEUED)
                .createdAt(Instant.now())
                .message("Event published successfully")
                .build();

        when(eventService.publish(any(PublishEventRequest.class))).thenReturn(resp);

        PublishEventRequest body = PublishEventRequest.builder()
                .eventType(NotificationEvent.EventType.ORDER_PLACED)
                .payload("{\"orderId\":\"123\"}")
                .tenantId("tenant-abc")
                .channels(List.of(NotificationLog.Channel.WEBSOCKET))
                .build();

        mockMvc.perform(post("/api/events/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.message").value("Event published successfully"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    @DisplayName("POST /api/events/publish with missing payload → 400")
    void publish_missingPayload_returns400() throws Exception {
        PublishEventRequest body = PublishEventRequest.builder()
                .eventType(NotificationEvent.EventType.ORDER_PLACED)
                // payload intentionally omitted
                .tenantId("tenant-abc")
                .channels(List.of(NotificationLog.Channel.WEBSOCKET))
                .build();

        mockMvc.perform(post("/api/events/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/events/publish without authentication → 401")
    void publish_unauthenticated_returns401() throws Exception {
        PublishEventRequest body = PublishEventRequest.builder()
                .eventType(NotificationEvent.EventType.ORDER_PLACED)
                .payload("{\"orderId\":\"123\"}")
                .tenantId("tenant-abc")
                .channels(List.of(NotificationLog.Channel.WEBSOCKET))
                .build();

        mockMvc.perform(post("/api/events/publish")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }
}
