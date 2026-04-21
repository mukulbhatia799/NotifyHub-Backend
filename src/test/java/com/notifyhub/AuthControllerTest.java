package com.notifyhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notifyhub.dto.AuthResponse;
import com.notifyhub.dto.LoginRequest;
import com.notifyhub.dto.RefreshTokenRequest;
import com.notifyhub.dto.RegisterRequest;
import com.notifyhub.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.notifyhub.controller.AuthController;
import com.notifyhub.security.JwtAuthFilter;
import com.notifyhub.security.JwtUtil;
import org.springframework.context.annotation.Import;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController — register / login / refresh")
class AuthControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService   authService;
    @MockBean JwtUtil       jwtUtil;
    @MockBean JwtAuthFilter jwtAuthFilter;
    @MockBean org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private static final AuthResponse DUMMY_RESPONSE = AuthResponse.builder()
            .accessToken("access.token.here")
            .refreshToken("refresh-token-uuid")
            .tokenType("Bearer")
            .expiresIn(900_000L)
            .userId("user-uuid")
            .email("user@example.com")
            .role("USER")
            .build();

    @Test
    @DisplayName("POST /api/auth/register → 201 with auth tokens")
    void register_returnsCreatedWithTokens() throws Exception {
        when(authService.register(any(RegisterRequest.class))).thenReturn(DUMMY_RESPONSE);

        RegisterRequest body = RegisterRequest.builder()
                .name("Test User")
                .email("user@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access.token.here"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /api/auth/register with invalid payload → 400")
    void register_invalidPayload_returns400() throws Exception {
        RegisterRequest body = RegisterRequest.builder()
                .name("")          // blank → invalid
                .email("not-email")
                .password("short")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login → 200 with auth tokens")
    void login_returnsOkWithTokens() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(DUMMY_RESPONSE);

        LoginRequest body = LoginRequest.builder()
                .email("user@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @DisplayName("POST /api/auth/refresh → 200 with new access token")
    void refresh_returnsNewAccessToken() throws Exception {
        AuthResponse refreshed = AuthResponse.ofAccess("new.access.token", 900_000L);
        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(refreshed);

        RefreshTokenRequest body = RefreshTokenRequest.builder()
                .refreshToken("refresh-token-uuid")
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"));
    }
}
