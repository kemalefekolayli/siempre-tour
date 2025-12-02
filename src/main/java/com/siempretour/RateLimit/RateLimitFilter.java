package com.siempretour.RateLimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = getClientIp(request);

        Bucket bucket;
        String identifier;

        // Auth endpoints - rate limit by IP (stricter)
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            bucket = rateLimitConfig.resolveAuthBucket(clientIp);
            identifier = "IP:" + clientIp;
        }
        // API endpoints - rate limit by user if authenticated, otherwise by IP
        else if (path.startsWith("/api/")) {
            Long userId = getCurrentUserId();
            if (userId != null) {
                bucket = rateLimitConfig.resolveApiBucket(String.valueOf(userId));
                identifier = "User:" + userId;
            } else {
                bucket = rateLimitConfig.resolveApiBucket(clientIp);
                identifier = "IP:" + clientIp;
            }
        }
        // Other endpoints - no rate limiting
        else {
            filterChain.doFilter(request, response);
            return;
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Add rate limit headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitTimeSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

            log.warn("Rate limit exceeded for {} on path {}", identifier, path);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitTimeSeconds));
            response.getWriter().write("""
                {
                    "error": "Too many requests",
                    "message": "Rate limit exceeded. Please try again later.",
                    "retryAfterSeconds": %d
                }
                """.formatted(waitTimeSeconds));
        }
    }

    private String getClientIp(HttpServletRequest request) {
        // Check for proxy headers (Railway uses proxies)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take first IP if multiple
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof Long) {
                return (Long) authentication.getPrincipal();
            }
        } catch (Exception ignored) {
            // Not authenticated
        }
        return null;
    }
}