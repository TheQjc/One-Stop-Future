package com.campus.service;

import java.time.Duration;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.campus.config.RedisIntegrationProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PlatformCacheService {

    private static final Logger log = LoggerFactory.getLogger(PlatformCacheService.class);

    private final RedisIntegrationProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PlatformCacheService(RedisIntegrationProperties properties, StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T getOrLoad(String key, TypeReference<T> typeReference, Supplier<T> loader) {
        if (!properties.isEnabled()) {
            return loader.get();
        }

        boolean cacheReadable = true;
        try {
            String cachedJson = redisTemplate.opsForValue().get(key);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, typeReference);
            }
        } catch (RuntimeException exception) {
            cacheReadable = false;
            log.warn("Redis cache failed for key {}, falling back to source query", key, exception);
        } catch (Exception exception) {
            cacheReadable = false;
            log.warn("Redis cache serialization failed for key {}, falling back to source query", key, exception);
        }

        T value = loader.get();
        if (!cacheReadable) {
            return value;
        }
        try {
            Duration ttl = Duration.ofSeconds(Math.max(1, properties.getTtlSeconds()));
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException exception) {
            log.warn("Redis cache write failed for key {}, source result has been returned", key, exception);
        } catch (Exception exception) {
            log.warn("Redis cache serialization failed for key {}, source result has been returned", key, exception);
        }
        return value;
    }
}
