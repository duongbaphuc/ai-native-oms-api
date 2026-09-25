// AI Provenance: generated from docs/02-observability-and-logging.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for {@link CorrelationIdFilter}.
 *
 * @apiNote Verifies distributed tracing correlation extraction, UUID generation fallbacks,
 *          response header propagation, dual MDC key population, and ThreadLocal cleanup.
 * @author GPC OMS Architecture Team
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("CorrelationIdFilter Unit Tests")
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Nested
    @DisplayName("Header Extraction and UUID Generation")
    class HeaderExtractionTests {

        @Test
        @DisplayName("Generates new UUID and populates response and MDC when header is missing")
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

            // Verify MDC cleanup
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("Preserves and trims client-supplied X-Correlation-Id header")
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

            // Verify MDC cleanup
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("Generates new UUID when client-supplied header contains only whitespace")
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

            // Verify MDC cleanup
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }
    }

    @Nested
    @DisplayName("ThreadLocal Lifecycle & Exception Handling")
    class LifecycleAndExceptionTests {

        @Test
        @DisplayName("Ensures MDC cleanup even when downstream filter chain throws exception")
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

            // Verify MDC was cleaned up despite the exception
            assertThat(MDC.get(CorrelationIdFilter.TRACE_ID_MDC_KEY)).isNull();
            assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
        }
    }
}
