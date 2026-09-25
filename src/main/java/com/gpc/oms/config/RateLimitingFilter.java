// AI Provenance: generated from docs/security-auth-spec.md, docs/security-rules.md, docs/SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * HTTP Servlet Filter enforcing Token Bucket rate limiting and traffic throttling on REST endpoints.
 *
 * <p>Mitigates CWE-770 (Allocation of Resources Without Limits or Throttling) and DoS / Brute-force attacks
 * by enforcing HTTP method-based token consumption policies on client IP addresses.</p>
 *
 * @apiNote Applies exclusively to {@code /api/v1/workorders/**} URI paths. Read operations ({@code GET})
 *          are throttled at 60 requests per minute; write operations ({@code POST}, {@code PATCH}) are
 *          throttled at 20 requests per minute. Exceeding the policy returns HTTP 429 Too Many Requests
 *          with RFC 7807 Problem Details and a {@code Retry-After} HTTP response header.
 * @implSpec Extends {@link OncePerRequestFilter} to guarantee idempotent single execution per dispatch.
 *           Uses thread-safe {@link ConcurrentHashMap} storage with memory capacity eviction thresholds.
 * @implNote Non-blocking token consumption via Bucket4j lock-free atomic CAS primitives.
 * @author GPC OMS Architecture Team
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://cwe.mitre.org/data/definitions/770.html">CWE-770</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7807">RFC 7807 Problem Details</a>
 */
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private static final String TARGET_PATH_PREFIX = "/api/v1/workorders";
    private static final String PROBLEM_JSON_CONTENT_TYPE = "application/problem+json;charset=UTF-8";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final int MAX_CACHE_ENTRIES = 10_000;

    static final long READ_CAPACITY = 60L;
    static final long WRITE_CAPACITY = 20L;
    static final Duration REFILL_DURATION = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Default constructor for Spring Component instantiation.
     */
    public RateLimitingFilter() {
        super();
    }

    /**
     * Determines whether the given HTTP request should be excluded from rate limiting.
     *
     * @param request Current HTTP request
     * @return {@code true} if the request URI does not target {@code /api/v1/workorders/**}
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith(TARGET_PATH_PREFIX);
    }

    /**
     * Intercepts incoming requests, evaluates the token bucket for the client, and either allows
     * execution or halts the chain with HTTP 429 Too Many Requests.
     *
     * @param request The servlet request
     * @param response The servlet response
     * @param filterChain The filter chain
     * @throws ServletException if an error occurs during filter processing
     * @throws IOException if an I/O error occurs writing the error response
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        boolean isRead = HttpMethod.GET.matches(request.getMethod());
        String cacheKey = clientIp + ":" + (isRead ? "READ" : "WRITE");

        if (buckets.size() > MAX_CACHE_ENTRIES) {
            log.warn("RateLimitingFilter cache exceeded capacity limit [size={}]. Evicting entries to prevent OOM.", buckets.size());
            buckets.clear();
        }

        Bucket bucket = buckets.computeIfAbsent(cacheKey, key -> createNewBucket(isRead));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long waitForRefillNanos = probe.getNanosToWaitForRefill();
        long retryAfterSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(waitForRefillNanos));

        log.warn("Rate limit exceeded for client [ip={}, method={}, uri={}, retryAfterSeconds={}]",
                clientIp, request.getMethod(), request.getRequestURI(), retryAfterSeconds);

        response.setStatus(429);
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
        response.setContentType(PROBLEM_JSON_CONTENT_TYPE);

        String problemJson = """
            {"type":"urn:problem-type:rate-limit-exceeded","title":"Too Many Requests","status":429,"detail":"Bạn đã vượt quá giới hạn tần suất gọi API. Vui lòng thử lại sau %d giây.","instance":"%s"}"""
            .formatted(retryAfterSeconds, request.getRequestURI());

        response.getWriter().write(problemJson);
    }

    /**
     * Resolves the originating client IP address, evaluating {@code X-Forwarded-For} before falling back
     * to {@link HttpServletRequest#getRemoteAddr()}.
     *
     * @param request The current HTTP request
     * @return The canonical IP address string
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "unknown-client";
    }

    /**
     * Instantiates a new {@link Bucket} configured with the appropriate bandwidth limits.
     *
     * @param isRead {@code true} for read operations (60 req/min), {@code false} for write operations (20 req/min)
     * @return A thread-safe {@link Bucket} instance
     */
    private Bucket createNewBucket(boolean isRead) {
        long capacity = isRead ? READ_CAPACITY : WRITE_CAPACITY;
        Bandwidth bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, REFILL_DURATION)
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}
