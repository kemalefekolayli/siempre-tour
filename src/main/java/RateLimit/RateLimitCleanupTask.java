package RateLimit;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class RateLimitCleanupTask {

    private final RateLimitConfig rateLimitConfig;

    /**
     * Clean up old buckets every hour to prevent memory leaks
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupBuckets() {
        log.info("Running rate limit bucket cleanup");
        rateLimitConfig.clearOldBuckets();
    }
}