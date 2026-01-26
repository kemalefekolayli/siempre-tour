package com.siempretour.RateLimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    // Store buckets per IP address
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // Store buckets per user (for authenticated endpoints)
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * Rate limit for auth endpoints (login/register)
     * 10 requests per minute per IP - strict to prevent brute force
     */
    public Bucket resolveAuthBucket(String ip) {
        if (!rateLimitEnabled) {
            return createUnlimitedBucket();
        }
        return ipBuckets.computeIfAbsent(ip, k -> createAuthBucket());
    }

    /**
     * Rate limit for general API endpoints
     * 100 requests per minute per user
     */
    public Bucket resolveApiBucket(String userIdOrIp) {
        if (!rateLimitEnabled) {
            return createUnlimitedBucket();
        }
        return userBuckets.computeIfAbsent(userIdOrIp, k -> createApiBucket());
    }

    /**
     * Rate limit for expensive operations (reports, bulk queries)
     * 10 requests per minute
     */
    public Bucket resolveHeavyBucket(String userIdOrIp) {
        if (!rateLimitEnabled) {
            return createUnlimitedBucket();
        }
        return userBuckets.computeIfAbsent("heavy_" + userIdOrIp, k -> createHeavyBucket());
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createHeavyBucket() {
        Bandwidth limit = Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Create a very high capacity bucket for testing (effectively unlimited)
     * Uses 1 billion tokens per minute to avoid exceeding Bucket4j's max rate
     */
    private Bucket createUnlimitedBucket() {
        Bandwidth limit = Bandwidth.classic(1_000_000_000L, Refill.greedy(1_000_000_000L, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Clean up old buckets periodically (call from scheduled task)
     */
    public void clearOldBuckets() {
        // Simple cleanup - in production you might want smarter eviction
        if (ipBuckets.size() > 10000) {
            ipBuckets.clear();
        }
        if (userBuckets.size() > 10000) {
            userBuckets.clear();
        }
    }
}
