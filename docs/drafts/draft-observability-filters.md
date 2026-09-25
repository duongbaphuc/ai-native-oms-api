<!--
Role: Senior Engineer. Task: Tạo các bộ lọc CorrelationIdFilter và RateLimitingFilter phục vụ giám sát phân tán và kiểm soát lưu lượng chống quá tải.
Context files: docs/00-security-rules.md, docs/02-observability-and-logging.md, docs/09-SECURITY_HANDOVER_REPORT.md
Constraints: 
- CorrelationIdFilter: OncePerRequestFilter, HIGHEST_PRECEDENCE, header X-Correlation-Id, MDC traceId & correlationId, MDC.clear() trong khối finally.
- RateLimitingFilter: OncePerRequestFilter, @Order(1), Bucket4j Token Bucket theo Client IP, Read (60 req/min), Write (20 req/min).
- shouldNotFilter: Bỏ qua /actuator/**, /, /index.html, /favicon.ico, /h2-console/**, chỉ áp dụng cho /api/v1/workorders/**.
- RFC 7807 response khi vượt hạn mức: 429 Too Many Requests kèm Retry-After header.
- Cơ chế giải phóng bộ nhớ chống rò rỉ (CWE-400): ngưỡng MAX_CACHE_ENTRIES = 10,000.
- Pure Java 17, Zero Lombok, Constructor Injection, final modifiers.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Distributed Observability & Rate Limiting Filters

## 1. Target Files

| File | Package | Path | Action | Mục Đích |
|---|---|---|---|---|
| `CorrelationIdFilter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/CorrelationIdFilter.java` | NEW | Trích xuất hoặc sinh mã `X-Correlation-Id`, nạp SLF4J MDC `traceId` & `correlationId`, dọn dẹp ThreadLocal |
| `RateLimitingFilter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/RateLimitingFilter.java` | NEW | Giới hạn tốc độ truy cập REST theo thuật toán Token Bucket của Bucket4j cho từng địa chỉ IP máy trạm |
| `CorrelationIdFilterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/CorrelationIdFilterTest.java` | NEW | Kiểm thử vòng đời filter, kế thừa correlation ID có sẵn, sinh UUID mới, và dọn dẹp MDC khi gặp lỗi |
| `RateLimitingFilterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/RateLimitingFilterTest.java` | NEW | Kiểm thử chính sách Đọc (60 req/min), Ghi (20 req/min), mã lỗi 429 RFC 7807, và cơ chế shouldNotFilter |

---

## 2. Architecture & Step-by-Step Logic

```
   Incoming HTTP Request
            │
            ▼
┌────────────────────────────────────────────────────────┐
│ CorrelationIdFilter (@Order(HIGHEST_PRECEDENCE))       │
│ 1. Đọc header "X-Correlation-Id" từ request             │
│    - Nếu có giá trị hợp lệ -> sử dụng                   │
│    - Nếu vắng mặt/rỗng    -> sinh UUID.randomUUID()     │
│ 2. MDC.put("traceId", correlationId)                   │
│    MDC.put("correlationId", correlationId)             │
│ 3. Ghi header response: "X-Correlation-Id"             │
│ 4. Chuyển tiếp filterChain.doFilter(...)                │
│ 5. Khối finally: MDC.clear() (Triệt tiêu rò rỉ bộ nhớ) │
└───────────┬────────────────────────────────────────────┘
            │
            ▼
┌────────────────────────────────────────────────────────┐
│ RateLimitingFilter (@Order(1))                         │
│ 1. shouldNotFilter(request):                           │
│    - Bỏ qua nếu URI KHÔNG bắt đầu bằng                 │
│      "/api/v1/workorders" (Actuator, H2 console, UI)   │
│ 2. Phân giải Client IP (hỗ trợ X-Forwarded-For)        │
│ 3. Lấy hoặc tạo Bucket cho Client IP + HTTP Method:    │
│    - GET: 60 tokens / 1 phút (Read Policy)             │
│    - POST, PATCH: 20 tokens / 1 phút (Write Policy)    │
│ 4. bucket.tryConsumeAndReturnRemaining(1):             │
│    - Đủ token: filterChain.doFilter(...)               │
│    - Hết token: Ghi HTTP 429 Too Many Requests         │
│      * Response Content-Type: application/problem+json │
│      * Header: Retry-After: <số giây cần chờ>          │
│      * RFC 7807 Body: type=urn:problem-type:rate-limit-│
│        exceeded, status=429                            │
└────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> **Vệ Sinh Vòng Đời ThreadLocal (CWE-778 & Memory Leak Mitigation):**
> Trong môi trường servlet container (Tomcat) dùng worker thread pool, các thread được tái sử dụng liên tục cho nhiều request khác nhau. Khối `try ... finally { MDC.clear(); }` là BẮT BUỘC để ngăn ngừa việc rò rỉ ngữ cảnh log của người dùng này sang phiên làm việc của người dùng khác.

> [!WARNING]
> **Chống Tấn Công Từ Chối Dịch Vụ Cạn Kiệt Bộ Nhớ (CWE-400 Mitigation):**
> Bộ nhớ đệm `ConcurrentHashMap<String, Bucket>` được kiểm soát ngưỡng tối đa `MAX_CACHE_ENTRIES = 10_000`. Khi đạt ngưỡng (do botnet spoofing hàng loạt IP), bộ đệm tự động kích hoạt cơ chế `clear()` để bảo vệ heap JVM không bị tràn bộ nhớ (OutOfMemoryError).

---

## 3. Implementation Blueprint: `CorrelationIdFilter.java`

```java
// AI Provenance: generated from docs/02-observability-and-logging.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    public CorrelationIdFilter() {
        super();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        } else {
            correlationId = correlationId.trim();
        }

        try {
            MDC.put(TRACE_ID_MDC_KEY, correlationId);
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
```

---

## 4. Implementation Blueprint: `RateLimitingFilter.java`

```java
// AI Provenance: generated from docs/02-security-auth-spec.md, docs/00-security-rules.md, docs/09-SECURITY_HANDOVER_REPORT.md
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

    public RateLimitingFilter() {
        super();
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        return path == null || !path.startsWith(TARGET_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        final String clientIp = resolveClientIp(request);
        final boolean isRead = HttpMethod.GET.matches(request.getMethod());
        final String cacheKey = clientIp + ":" + (isRead ? "READ" : "WRITE");

        if (buckets.size() > MAX_CACHE_ENTRIES) {
            log.warn("RateLimitingFilter cache exceeded capacity limit [size={}]. Evicting entries to prevent OOM.", buckets.size());
            buckets.clear();
        }

        final Bucket bucket = buckets.computeIfAbsent(cacheKey, key -> createNewBucket(isRead));
        final ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        final long waitForRefillNanos = probe.getNanosToWaitForRefill();
        final long retryAfterSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(waitForRefillNanos));

        log.warn("Rate limit exceeded for client [ip={}, method={}, uri={}, retryAfterSeconds={}]",
                clientIp, request.getMethod(), request.getRequestURI(), retryAfterSeconds);

        response.setStatus(429);
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
        response.setContentType(PROBLEM_JSON_CONTENT_TYPE);

        final String problemJson = """
            {"type":"urn:problem-type:rate-limit-exceeded","title":"Too Many Requests","status":429,"detail":"Bạn đã vượt quá giới hạn tần suất gọi API. Vui lòng thử lại sau %d giây.","instance":"%s"}"""
            .formatted(retryAfterSeconds, request.getRequestURI());

        response.getWriter().write(problemJson);
    }

    private Bucket createNewBucket(final boolean isRead) {
        final long capacity = isRead ? READ_CAPACITY : WRITE_CAPACITY;
        final Bandwidth bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, REFILL_DURATION)
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private String resolveClientIp(final HttpServletRequest request) {
        final String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            final String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        final String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : "unknown-client";
    }
}
```

---

## 5. Filter Test Sketches

### Sketch 1: `CorrelationIdFilterTest.java`
- **Mục tiêu:**
  1. Kiểm tra request đến không có header thì sinh mã UUID ngẫu nhiên và gán vào header phản hồi.
  2. Kiểm tra request đến đã có header `X-Correlation-Id` thì giữ nguyên giá trị đã cung cấp.
  3. Kiểm tra MDC chứa đầy đủ `traceId` và `correlationId` trong quá trình thực thi và bị dọn dẹp hoàn toàn sau khi xử lý xong (kể cả khi downstream ném Exception).

### Sketch 2: `RateLimitingFilterTest.java`
- **Mục tiêu:**
  1. Kiểm tra chính sách GET cho phép 60 lượt gửi và từ chối ở lượt thứ 61 với HTTP 429.
  2. Kiểm tra chính sách POST/PATCH cho phép 20 lượt gửi và từ chối ở lượt thứ 21 với HTTP 429.
  3. Kiểm tra header `Retry-After` và định dạng JSON RFC 7807 trong phản hồi 429.
  4. Kiểm tra các URI ngoài phạm vi (`/actuator/health`, `/h2-console`, `/`) không bị can thiệp bởi filter.
