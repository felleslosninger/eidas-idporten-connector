package no.idporten.eidas.connector.integration.specificcommunication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.digdir.oidc.redis.metrics.RedisMetrics;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class LightRedisCache<K, V> {

    private final RedisTemplate<K, V> redisTemplate;
    private final RedisMetrics redisMetrics;

    public V set(K cacheKey, V value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, duration);
            return value;
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to set {} object in cache: {}", cacheKey, e.getMessage());
            redisMetrics.incrementTimeoutCount();
            throw e;
        }
    }

    public V get(K cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to get {} object from cache: {}", cacheKey, e.getMessage());
            redisMetrics.incrementTimeoutCount();
            throw e;
        }
    }

    public V delete(K cacheKey) {
        try {
            return redisTemplate.opsForValue().getAndDelete(cacheKey);
        } catch (RedisConnectionFailureException | QueryTimeoutException e) {
            log.error("Failed to delete {} object from cache: {}", cacheKey, e.getMessage());
            redisMetrics.incrementTimeoutCount();
            throw e;
        }
    }
}
