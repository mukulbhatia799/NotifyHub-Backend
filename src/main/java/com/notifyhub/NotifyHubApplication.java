package com.notifyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(exclude = RedisRepositoriesAutoConfiguration.class)
public class NotifyHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyHubApplication.class, args);
    }
}
