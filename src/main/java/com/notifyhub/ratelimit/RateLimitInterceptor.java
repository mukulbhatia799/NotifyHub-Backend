package com.notifyhub.ratelimit;

import com.notifyhub.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "rate::";

    private final StringRedisTemplate stringRedisTemplate;
    private final int                  maxRequests;
    private final long                 windowSeconds;

    public RateLimitInterceptor(
            StringRedisTemplate stringRedisTemplate,
            @Value("${app.rate-limit.max-requests}") int maxRequests,
            @Value("${app.rate-limit.window-seconds}") long windowSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.maxRequests         = maxRequests;
        this.windowSeconds       = windowSeconds;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        String userId = resolveUserId();
        String key    = KEY_PREFIX + userId;

        Long count = stringRedisTemplate.opsForValue().increment(key);

        if (count == null) {
            // Redis unavailable — fail open to avoid blocking legitimate traffic
            log.warn("Redis unavailable during rate limit check for userId={}", userId);
            return true;
        }

        if (count == 1) {
            // First request in this window — set TTL
            stringRedisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }

        if (count > maxRequests) {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            long retryAfter = (ttl != null && ttl > 0) ? ttl : windowSeconds;
            log.info("Rate limit exceeded for userId={} count={}", userId, count);
            throw new RateLimitException(retryAfter);
        }

        return true;
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
