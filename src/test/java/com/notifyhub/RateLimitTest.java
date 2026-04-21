package com.notifyhub;

import com.notifyhub.exception.RateLimitException;
import com.notifyhub.ratelimit.RateLimitInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitInterceptor — Redis INCR + EXPIRE rate limiting")
class RateLimitTest {

    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    private RateLimitInterceptor interceptor;

    private static final int  MAX_REQUESTS    = 10;
    private static final long WINDOW_SECONDS  = 60L;
    private static final String USER_EMAIL    = "user@example.com";

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor(stringRedisTemplate, MAX_REQUESTS, WINDOW_SECONDS);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        // Authenticate the security context
        Authentication auth = new UsernamePasswordAuthenticationToken(
                USER_EMAIL, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("First 10 requests pass through without exception")
    void tenRequests_allPass() {
        for (int i = 1; i <= MAX_REQUESTS; i++) {
            when(valueOps.increment("rate::" + USER_EMAIL)).thenReturn((long) i);
            if (i == 1) {
                when(stringRedisTemplate.expire(anyString(), any())).thenReturn(true);
            }

            boolean result = interceptor.preHandle(
                    new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("11th request in the same window throws RateLimitException with 429")
    void eleventhRequest_throwsRateLimitException() {
        long overLimit = MAX_REQUESTS + 1L;
        when(valueOps.increment("rate::" + USER_EMAIL)).thenReturn(overLimit);
        when(stringRedisTemplate.getExpire("rate::" + USER_EMAIL, TimeUnit.SECONDS))
                .thenReturn(42L);

        assertThatThrownBy(() ->
                interceptor.preHandle(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        new Object()))
                .isInstanceOf(RateLimitException.class)
                .hasMessageContaining("Rate limit exceeded")
                .hasMessageContaining("42 seconds");
    }

    @Test
    @DisplayName("retryAfterSeconds falls back to windowSeconds when TTL is unavailable")
    void retryAfter_fallsBackToWindowSeconds_whenTtlNull() {
        when(valueOps.increment("rate::" + USER_EMAIL)).thenReturn((long) MAX_REQUESTS + 1);
        when(stringRedisTemplate.getExpire("rate::" + USER_EMAIL, TimeUnit.SECONDS)).thenReturn(null);

        assertThatThrownBy(() ->
                interceptor.preHandle(
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        new Object()))
                .isInstanceOf(RateLimitException.class)
                .satisfies(e -> assertThat(((RateLimitException) e).getRetryAfterSeconds())
                        .isEqualTo(WINDOW_SECONDS));
    }

    @Test
    @DisplayName("Redis unavailable (null count) — fails open and allows request")
    void redisUnavailable_failsOpen() {
        when(valueOps.increment("rate::" + USER_EMAIL)).thenReturn(null);

        boolean result = interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
    }
}
