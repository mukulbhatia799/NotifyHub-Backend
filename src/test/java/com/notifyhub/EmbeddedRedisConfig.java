package com.notifyhub;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

import java.io.IOException;

/**
 * Starts an embedded Redis server on port 6370 for integration tests.
 * Uses a non-default port to avoid conflicts with a locally running Redis.
 */
@TestConfiguration
public class EmbeddedRedisConfig {

    private static final int REDIS_TEST_PORT = 6370;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        redisServer = new RedisServer(REDIS_TEST_PORT);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}
