package com.strengthlabs.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sliding-window rate limiter for auth endpoints. In-memory only — adequate for
 * the academic scope; a production deployment should back this with Redis.
 *
 * Policy: max attempts per (IP + window). Window restarts when first hit ages out.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/auth/login", "/auth/register", "/auth/refresh"
    );

    private final int maxAttempts;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(@Value("${security.rate-limit.max-attempts:5}") int maxAttempts,
                                 @Value("${security.rate-limit.window-minutes:15}") int windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMinutes * 60_000L;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String key = clientKey(request);
        long now = System.currentTimeMillis();

        Window window = attempts.compute(key, (k, existing) -> {
            if (existing == null || now - existing.firstHitAt > windowMillis) {
                return new Window(now);
            }
            existing.count.incrementAndGet();
            return existing;
        });

        int currentCount = window.count.get();
        if (currentCount > maxAttempts) {
            long retryAfterSec = Math.max(1L, (window.firstHitAt + windowMillis - now) / 1000);
            log.warn("Rate limit exceeded for {} on {} ({} attempts in window)",
                    key, request.getRequestURI(), currentCount);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSec));
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"error\":\"Too many attempts. Try again in %d seconds.\"}", retryAfterSec));
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Used by tests to start from a clean slate. */
    public void reset() {
        attempts.clear();
    }

    /** Snapshot for diagnostics / tests. */
    public Map<String, Integer> snapshot() {
        Map<String, Integer> view = new java.util.LinkedHashMap<>();
        attempts.forEach((k, v) -> view.put(k, v.count.get()));
        return view;
    }

    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long firstHitAt;
        final AtomicInteger count = new AtomicInteger(1);
        Window(long firstHitAt) { this.firstHitAt = firstHitAt; }
    }
}
