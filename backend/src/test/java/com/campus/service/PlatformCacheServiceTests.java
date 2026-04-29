package com.campus.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.campus.config.RedisIntegrationProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

class PlatformCacheServiceTests {

    @Test
    void disabledCacheUsesLoaderWithoutTouchingRedis() {
        RedisIntegrationProperties properties = new RedisIntegrationProperties();
        properties.setEnabled(false);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        PlatformCacheService cacheService = new PlatformCacheService(properties, redisTemplate, objectMapper());

        String value = cacheService.getOrLoad("key", new TypeReference<String>() {
        }, () -> "source");

        assertThat(value).isEqualTo("source");
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void cacheHitReturnsRedisValue() {
        RedisIntegrationProperties properties = enabledProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get("key")).thenReturn("\"cached\"");
        PlatformCacheService cacheService = new PlatformCacheService(properties, redisTemplate, objectMapper());

        String value = cacheService.getOrLoad("key", new TypeReference<String>() {
        }, () -> "source");

        assertThat(value).isEqualTo("cached");
    }

    @Test
    void redisFailureFallsBackToLoaderOnce() {
        RedisIntegrationProperties properties = enabledProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        PlatformCacheService cacheService = new PlatformCacheService(properties, redisTemplate, objectMapper());
        AtomicInteger loads = new AtomicInteger();

        String value = cacheService.getOrLoad("key", new TypeReference<String>() {
        }, () -> {
            loads.incrementAndGet();
            return "source";
        });

        assertThat(value).isEqualTo("source");
        assertThat(loads).hasValue(1);
    }

    @Test
    void cacheMissWritesLoadedValue() {
        RedisIntegrationProperties properties = enabledProperties();
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        when(operations.get("key")).thenReturn(null);
        PlatformCacheService cacheService = new PlatformCacheService(properties, redisTemplate, objectMapper());

        String value = cacheService.getOrLoad("key", new TypeReference<String>() {
        }, () -> "source");

        assertThat(value).isEqualTo("source");
        verify(operations).set(eq("key"), eq("\"source\""), any(Duration.class));
    }

    private RedisIntegrationProperties enabledProperties() {
        RedisIntegrationProperties properties = new RedisIntegrationProperties();
        properties.setEnabled(true);
        properties.setTtlSeconds(30);
        return properties;
    }

    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }
}
