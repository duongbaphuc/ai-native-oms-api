// Nguồn gốc AI: sinh từ docs/02-observability-and-logging.md, docs/09-SECURITY_HANDOVER_REPORT.md
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

/**
 * Bộ lọc HTTP Servlet có độ ưu tiên cao nhất, thiết lập mã định danh truy vết tương quan và ngữ cảnh SLF4J MDC.
 *
 * <p>Giảm thiểu lỗ hổng CWE-778 (Ghi nhật ký không đầy đủ) và phục vụ kiểm toán kỹ thuật bằng cách bảo đảm
 * mọi yêu cầu gửi đến hệ thống đều được cấp phát định danh duy nhất xác định hoặc tự sinh ngẫu nhiên.</p>
 *
 * @apiNote Chặn mọi yêu cầu HTTP trước chuỗi bảo mật hoặc bộ điều phối ứng dụng.
 *          Nếu client cung cấp tiêu đề {@code X-Correlation-Id}, giá trị hợp lệ sẽ được tái sử dụng;
 *          nếu không, mã UUID ngẫu nhiên (RFC 4122) sẽ được tạo mới. Cả hai khóa {@code traceId}
 *          và {@code correlationId} đều được đưa vào MDC, đồng thời tiêu đề được gắn vào phản hồi HTTP.
 * @implSpec Kế thừa {@link OncePerRequestFilter} đảm bảo mỗi yêu cầu chỉ thực thi một lần duy nhất trên luồng.
 *           Được gắn nhãn {@link Ordered#HIGHEST_PRECEDENCE} để luôn chạy trước chuỗi lọc của Spring Security.
 * @implNote Thực thi dọn dẹp biến ThreadLocal nghiêm ngặt thông qua việc xóa khóa MDC trong khối lệnh
 *           {@code finally} vô điều kiện, ngăn ngừa ô nhiễm ngữ cảnh giữa các luồng worker được tái sử dụng.
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 * @see <a href="https://cwe.mitre.org/data/definitions/778.html">CWE-778</a>
 * @see org.slf4j.MDC
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /**
     * Tiêu đề HTTP tiêu chuẩn chứa mã định danh truy vết tương quan phân tán.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /**
     * Khóa ngữ cảnh SLF4J MDC tuân theo đặc tả Elastic Common Schema (ECS).
     */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    /**
     * Khóa ngữ cảnh SLF4J MDC phục vụ nhật ký kiểm toán nghiệp vụ sự cố lưới điện.
     */
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /**
     * Constructor mặc định phục vụ việc khởi tạo bean trong Spring container.
     */
    public CorrelationIdFilter() {
        super();
    }

    /**
     * Thực thi logic bộ lọc, thiết lập ngữ cảnh MDC và truyền tiếp tiêu đề tương quan.
     *
     * @param request Yêu cầu HTTP servlet hiện tại
     * @param response Phản hồi HTTP servlet đang được tạo lập
     * @param filterChain Chuỗi các bộ lọc tiếp theo
     * @throws ServletException nếu phát sinh lỗi trong quá trình xử lý chuỗi lọc phía sau
     * @throws IOException nếu phát sinh lỗi I/O khi ghi hoặc truyền dữ liệu phản hồi
     */
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
