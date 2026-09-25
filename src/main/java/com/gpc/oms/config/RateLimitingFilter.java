// Nguồn gốc AI: sinh từ docs/security-auth-spec.md, docs/security-rules.md, docs/SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gpc.oms.exception.ProblemTypes;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Bộ lọc HTTP Servlet thực thi giới hạn tần suất gọi API theo thuật toán thùng thẻ (Token Bucket Rate Limiting).
 *
 * <p>Giảm thiểu nguy cơ CWE-770 (Cấp phát tài nguyên không giới hạn) và tấn công từ chối dịch vụ (DoS)
 * bằng cách áp dụng hạn mức tiêu thụ thẻ dựa trên phương thức HTTP cho từng địa chỉ IP máy khách.</p>
 *
 * @apiNote Áp dụng độc quyền cho các tiền tố URI {@code /api/v1/workorders/**}. Thao tác đọc ({@code GET})
 *          được cấp hạn mức 60 yêu cầu/phút; thao tác ghi ({@code POST}, {@code PATCH}) được cấp 20 yêu cầu/phút.
 *          Khi vượt ngưỡng, hệ thống trả về HTTP 429 Too Many Requests kèm RFC 7807 Problem Details
 *          và tiêu đề {@code Retry-After} trong phản hồi HTTP.
 * @implSpec Kế thừa {@link OncePerRequestFilter} đảm bảo thực thi đúng một lần trên mỗi yêu cầu.
 *           Sử dụng bộ nhớ đệm Caffeine Cache an toàn luồng với chính sách giải phóng LRU / TinyLFU.
 * @implNote Tiêu thụ thẻ non-blocking thông qua các nguyên hàm atomic CAS không khóa của Bucket4j.
 * @author Đội ngũ Kiến trúc GPC OMS
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
    private static final String PROBLEM_JSON_CONTENT_TYPE =
            MediaType.APPLICATION_PROBLEM_JSON_VALUE + ";charset=UTF-8";
    private static final String RETRY_AFTER_HEADER = HttpHeaders.RETRY_AFTER;
    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String UNKNOWN_CLIENT = "unknown-client";

    static final long READ_CAPACITY = RateLimitProperties.DEFAULT_READ_CAPACITY;
    static final long WRITE_CAPACITY = RateLimitProperties.DEFAULT_WRITE_CAPACITY;
    static final Duration REFILL_DURATION = RateLimitProperties.DEFAULT_REFILL_DURATION;

    private final RateLimitProperties properties;
    private final Cache<String, Bucket> buckets;

    /**
     * Khởi tạo bộ lọc với các thuộc tính cấu hình được tiêm từ Spring context.
     *
     * @param properties Cấu hình thuộc tính giới hạn tần suất {@link RateLimitProperties}
     */
    public RateLimitingFilter(final RateLimitProperties properties) {
        super();
        this.properties = properties != null ? properties : new RateLimitProperties();
        this.buckets = Caffeine.newBuilder()
                .maximumSize(this.properties.cacheMaxSize())
                .expireAfterAccess(this.properties.cacheExpireDuration())
                .build();
    }

    /**
     * Constructor mặc định phục vụ việc khởi tạo bean với giá trị mặc định.
     */
    public RateLimitingFilter() {
        this(new RateLimitProperties());
    }

    /**
     * Xác định xem yêu cầu HTTP hiện tại có được miễn trừ kiểm tra giới hạn tần suất hay không.
     *
     * @param request Yêu cầu HTTP hiện tại
     * @return {@code true} nếu URI không thuộc phạm vi {@code /api/v1/workorders/**}; {@code false} nếu cần lọc
     */
    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        final String path = request.getRequestURI();
        return path == null || !path.startsWith(TARGET_PATH_PREFIX);
    }

    /**
     * Đánh chặn yêu cầu gửi đến, kiểm tra thùng thẻ của máy khách, và cho phép tiếp tục
     * hoặc ngắt chuỗi với mã lỗi HTTP 429 Too Many Requests.
     *
     * @param request Yêu cầu HTTP servlet
     * @param response Phản hồi HTTP servlet
     * @param filterChain Chuỗi các bộ lọc tiếp theo
     * @throws ServletException nếu phát sinh lỗi trong quá trình xử lý chuỗi lọc
     * @throws IOException nếu phát sinh lỗi I/O khi ghi phản hồi lỗi
     */
    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain) throws ServletException, IOException {

        final String clientIp = resolveClientIp(request);
        final boolean isRead = HttpMethod.GET.matches(request.getMethod());
        final String cacheKey = clientIp + ":" + (isRead ? "READ" : "WRITE");

        final Bucket bucket = buckets.get(cacheKey, key -> createNewBucket(isRead));
        final ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        final long waitForRefillNanos = probe.getNanosToWaitForRefill();
        final long retryAfterSeconds = Math.max(1L, TimeUnit.NANOSECONDS.toSeconds(waitForRefillNanos));

        log.warn("Rate limit exceeded for client [ip={}, method={}, uri={}, retryAfterSeconds={}]",
                clientIp, request.getMethod(), request.getRequestURI(), retryAfterSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
        response.setContentType(PROBLEM_JSON_CONTENT_TYPE);

        final String problemJson = """
            {"type":"%s",\
            "title":"%s",\
            "status":%d,\
            "detail":"Bạn đã vượt quá giới hạn tần suất gọi API. \
Vui lòng thử lại sau %d giây.",\
            "instance":"%s"}"""
            .formatted(ProblemTypes.RATE_LIMIT_EXCEEDED, ProblemTypes.TITLE_TOO_MANY_REQUESTS,
                    HttpStatus.TOO_MANY_REQUESTS.value(), retryAfterSeconds, request.getRequestURI());

        response.getWriter().write(problemJson);
    }

    /**
     * Phân giải địa chỉ IP máy khách, ưu tiên trích xuất từ tiêu đề {@code X-Forwarded-For}
     * trước khi sử dụng địa chỉ mạng trực tiếp {@link HttpServletRequest#getRemoteAddr()}.
     *
     * @param request Yêu cầu HTTP hiện tại
     * @return Chuỗi địa chỉ IP chuẩn hóa
     */
    private String resolveClientIp(final HttpServletRequest request) {
        final String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            final String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        final String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr.trim() : UNKNOWN_CLIENT;
    }

    /**
     * Khởi tạo đối tượng {@link Bucket} mới được cấu hình các tham số giới hạn băng thông tương ứng.
     *
     * @param isRead {@code true} cho thao tác đọc (60 req/phút), {@code false} cho thao tác ghi (20 req/phút)
     * @return Đối tượng {@link Bucket} an toàn luồng
     */
    private Bucket createNewBucket(final boolean isRead) {
        final long capacity = isRead ? properties.readCapacity() : properties.writeCapacity();
        final Bandwidth bandwidth = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, properties.refillDuration())
                .build();
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}
