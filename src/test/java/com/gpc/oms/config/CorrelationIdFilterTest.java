// Nguồn gốc AI: sinh từ docs/02-observability-and-logging.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Bộ kiểm thử đơn vị cho {@link CorrelationIdFilter}.
 *
 * <p>Kiểm tra trích xuất mã định danh truy vết tương quan, cơ chế fallback tự sinh UUID,
 * lan truyền tiêu đề phản hồi, đồng bộ hai khóa MDC và dọn dẹp biến ThreadLocal.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("Kiểm thử đơn vị bộ lọc CorrelationIdFilter")
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Nested
    @DisplayName("Trích xuất tiêu đề và tự sinh UUID")
    class HeaderExtractionTests {

        @Test
        @DisplayName("Tự sinh UUID mới và nạp vào phản hồi cùng MDC khi thiếu tiêu đề")
        void doFilter_withoutHeader_generatesUUID() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            AtomicReference<String> capturedTraceId = new AtomicReference<>();
            AtomicReference<String> capturedCorrelationId = new AtomicReference<>();

            FilterChain chain = (req, res) -> {
                capturedTraceId.set(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY));
                capturedCorrelationId.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
            };

            filter.doFilter(request, response, chain);

            String responseHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
            assertThat(responseHeader).isNotNull();
            assertThat(UUID.fromString(responseHeader)).isNotNull();

            assertThat(capturedTraceId.get()).isEqualTo(responseHeader);
            assertThat(capturedCorrelationId.get()).isEqualTo(responseHeader);

            // Xác nhận dọn dẹp MDC
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("Bảo toàn và loại bỏ khoảng trắng thừa của tiêu đề X-Correlation-Id từ client")
        void doFilter_withValidHeader_preservesCorrelationId() throws ServletException, IOException {
            String clientCorrelationId = "  custom-audit-trace-id-12345  ";
            String expectedId = "custom-audit-trace-id-12345";

            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/workorders");
            request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, clientCorrelationId);
            MockHttpServletResponse response = new MockHttpServletResponse();

            AtomicReference<String> capturedTraceId = new AtomicReference<>();
            AtomicReference<String> capturedCorrelationId = new AtomicReference<>();

            FilterChain chain = (req, res) -> {
                capturedTraceId.set(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY));
                capturedCorrelationId.set(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
            };

            filter.doFilter(request, response, chain);

            assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(expectedId);
            assertThat(capturedTraceId.get()).isEqualTo(expectedId);
            assertThat(capturedCorrelationId.get()).isEqualTo(expectedId);

            // Xác nhận dọn dẹp MDC
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("Tự sinh UUID mới khi tiêu đề từ client chỉ chứa toàn khoảng trắng")
        void doFilter_withBlankHeader_generatesUUID() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
            request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "    ");
            MockHttpServletResponse response = new MockHttpServletResponse();

            AtomicReference<String> capturedTraceId = new AtomicReference<>();

            FilterChain chain = (req, res) -> {
                capturedTraceId.set(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY));
            };

            filter.doFilter(request, response, chain);

            String responseHeader = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
            assertThat(responseHeader).isNotNull().isNotBlank();
            assertThat(UUID.fromString(responseHeader)).isNotNull();
            assertThat(capturedTraceId.get()).isEqualTo(responseHeader);

            // Xác nhận dọn dẹp MDC
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }
    }

    @Nested
    @DisplayName("Vòng đời ThreadLocal & Xử lý ngoại lệ")
    class LifecycleAndExceptionTests {

        @Test
        @DisplayName("Đảm bảo dọn dẹp MDC ngay cả khi chuỗi lọc phía sau ném ngoại lệ")
        void doFilter_downstreamThrowsException_cleansUpMdc() {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
            MockHttpServletResponse response = new MockHttpServletResponse();

            FilterChain failingChain = (req, res) -> {
                assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNotNull();
                throw new ServletException("Simulated downstream filter error");
            };

            assertThatThrownBy(() -> filter.doFilter(request, response, failingChain))
                    .isInstanceOf(ServletException.class)
                    .hasMessage("Simulated downstream filter error");

            // Xác nhận MDC đã được giải phóng bất chấp ngoại lệ
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }
    }
}
