// Nguồn gốc AI: sinh từ docs/00-coding-rules.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Thuộc tính cấu hình kiểu an toàn (Type-safe Configuration Properties)
 * cho bộ lọc giới hạn tần suất gọi API (Rate Limiting).
 *
 * <p>Externalize các tham số cấu hình thuật toán thùng thẻ Token Bucket
 * và bộ nhớ đệm Caffeine ra tệp {@code application.yml} với tiền tố {@code oms.rate-limit}.</p>
 *
 * @param readCapacity Hạn mức yêu cầu đọc (GET) tối đa trong một chu kỳ (mặc định 60)
 * @param writeCapacity Hạn mức yêu cầu ghi (POST, PATCH) tối đa trong một chu kỳ (mặc định 20)
 * @param refillDuration Chu kỳ nạp đầy lại thùng thẻ (mặc định 1 phút)
 * @param cacheMaxSize Dung lượng bản ghi tối đa của bộ nhớ đệm Caffeine (mặc định 10,000)
 * @param cacheExpireDuration Thời gian hết hạn giải phóng bộ nhớ đệm tính từ lần truy cập cuối (mặc định 10 phút)
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "oms.rate-limit")
public record RateLimitProperties(
        @DefaultValue("60") long readCapacity,
        @DefaultValue("20") long writeCapacity,
        @DefaultValue("1m") Duration refillDuration,
        @DefaultValue("10000") int cacheMaxSize,
        @DefaultValue("10m") Duration cacheExpireDuration
) {
    /** Hạn mức đọc mặc định: 60 yêu cầu/phút. */
    public static final long DEFAULT_READ_CAPACITY = 60L;

    /** Hạn mức ghi mặc định: 20 yêu cầu/phút. */
    public static final long DEFAULT_WRITE_CAPACITY = 20L;

    /** Chu kỳ nạp lại thẻ mặc định: 1 phút. */
    public static final Duration DEFAULT_REFILL_DURATION = Duration.ofMinutes(1);

    /** Kích thước cache tối đa mặc định: 10,000 địa chỉ IP. */
    public static final int DEFAULT_CACHE_MAX_SIZE = 10_000;

    /** Thời gian hết hạn cache mặc định: 10 phút. */
    public static final Duration DEFAULT_CACHE_EXPIRE_DURATION = Duration.ofMinutes(10);

    /**
     * Constructor mặc định khởi tạo với các giá trị tiêu chuẩn chuẩn mực.
     */
    public RateLimitProperties() {
        this(DEFAULT_READ_CAPACITY, DEFAULT_WRITE_CAPACITY, DEFAULT_REFILL_DURATION,
                DEFAULT_CACHE_MAX_SIZE, DEFAULT_CACHE_EXPIRE_DURATION);
    }
}
