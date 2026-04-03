package com.rishav.orderservice.cache;

import com.rishav.orderservice.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCacheService {

    private final RedisTemplate<String, OrderResponse> redisTemplate;

    private static final String CACHE_PREFIX = "order:";
    private static final Duration TTL = Duration.ofMinutes(30);

    public void cacheOrder(OrderResponse order) {
        String key = CACHE_PREFIX + order.getId();
        try {
            redisTemplate.opsForValue().set(key, order, TTL);
            log.debug("Cached order: {}", order.getId());
        } catch (Exception e) {
            log.warn("Failed to cache order {}: {}", order.getId(), e.getMessage());
        }
    }

    public Optional<OrderResponse> getCachedOrder(UUID orderId) {
        String key = CACHE_PREFIX + orderId;
        try {
            OrderResponse cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("Cache hit for order: {}", orderId);
                return Optional.of(cached);
            }
        } catch (Exception e) {
            log.warn("Cache read failed for order {}: {}", orderId, e.getMessage());
        }
        return Optional.empty();
    }

    public void evictOrder(UUID orderId) {
        String key = CACHE_PREFIX + orderId;
        try {
            redisTemplate.delete(key);
            log.debug("Evicted cache for order: {}", orderId);
        } catch (Exception e) {
            log.warn("Cache eviction failed for order {}: {}", orderId, e.getMessage());
        }
    }
}
