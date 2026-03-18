package com.strengthlabs.infrastructure.cache;

import com.strengthlabs.application.dtos.FatigueResultDTO;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class RedisMetricsCache {

    private static final String FATIGUE_KEY_PREFIX = "fatigue:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisMetricsCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheFatigue(UUID userId, FatigueResultDTO result) {
        redisTemplate.opsForValue().set(FATIGUE_KEY_PREFIX + userId, result, TTL);
    }

    public FatigueResultDTO getFatigue(UUID userId) {
        Object cached = redisTemplate.opsForValue().get(FATIGUE_KEY_PREFIX + userId);
        if (cached instanceof FatigueResultDTO dto) return dto;
        return null;
    }

    public void evictFatigue(UUID userId) {
        redisTemplate.delete(FATIGUE_KEY_PREFIX + userId);
    }
}
