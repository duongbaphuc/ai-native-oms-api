// Nguồn gốc AI: sinh từ docs/security-auth-spec.md, docs/security-rules.md, docs/SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Bộ kiểm thử đơn vị và kiểm thử phân lớp cho bộ lọc {@link RateLimitingFilter}.
 *
 * <p>Kiểm tra các bất biến thuật toán thùng thẻ Token Bucket (60 yêu cầu/phút cho GET, 20 yêu cầu/phút cho POST/PATCH),
 * cấu trúc phản hồi lỗi RFC 7807, tiêu đề Retry-After, cách ly địa chỉ IP máy khách và bộ nhớ đệm Caffeine.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("Kiểm thử đơn vị bộ lọc giới hạn tần suất RateLimitingFilter")
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    @Nested
    @DisplayName("Kiểm định loại trừ đường dẫn (shouldNotFilter)")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("Trả về true cho các endpoint không thuộc workorders (actuator, tài nguyên tĩnh, h2-console)")
        void shouldNotFilter_nonTargetPaths_returnsTrue() {
            MockHttpServletRequest actuatorReq = new MockHttpServletRequest("GET", "/actuator/health");
            MockHttpServletRequest h2Req = new MockHttpServletRequest("GET", "/h2-console");
            MockHttpServletRequest staticReq = new MockHttpServletRequest("GET", "/index.html");
            MockHttpServletRequest nullPathReq = new MockHttpServletRequest("GET", "");

            assertThat(filter.shouldNotFilter(actuatorReq)).isTrue();
            assertThat(filter.shouldNotFilter(h2Req)).isTrue();
            assertThat(filter.shouldNotFilter(staticReq)).isTrue();
            assertThat(filter.shouldNotFilter(nullPathReq)).isTrue();
        }

        @Test
        @DisplayName("Trả về false cho các endpoint thuộc /api/v1/workorders")
        void shouldNotFilter_workorderPaths_returnsFalse() {
            MockHttpServletRequest listReq = new MockHttpServletRequest("GET", "/api/v1/workorders");
            MockHttpServletRequest detailReq = new MockHttpServletRequest(
                    "GET", "/api/v1/workorders/123e4567-e89b-12d3-a456-426614174000");

            assertThat(filter.shouldNotFilter(listReq)).isFalse();
            assertThat(filter.shouldNotFilter(detailReq)).isFalse();
        }
    }

    @Nested
    @DisplayName("Chính sách giới hạn tần suất thao tác đọc (GET)")
    class ReadPolicyTests {

        @Test
        @DisplayName("Cho phép tối đa 60 yêu cầu GET mỗi phút cho cùng một địa chỉ IP")
        void getRequests_withinLimit_allPass() throws ServletException, IOException {
            String clientIp = "192.168.1.100";

            for (int i = 0; i < 60; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                FilterChain chain = mock(FilterChain.class);

                filter.doFilter(request, response, chain);

                assertThat(response.getStatus()).isEqualTo(200);
                verify(chain, times(1)).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Từ chối yêu cầu GET thứ 61 với HTTP 429, tiêu đề Retry-After và chi tiết lỗi RFC 7807")
        void getRequests_exceedingLimit_returns429() throws ServletException, IOException {
            String clientIp = "192.168.1.101";
            FilterChain chain = mock(FilterChain.class);

            // Dùng hết 60 thẻ token
            for (int i = 0; i < 60; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, chain);
            }

            // Yêu cầu thứ 61 phải bị chặn
            MockHttpServletRequest blockedRequest = new MockHttpServletRequest("GET", "/api/v1/workorders");
            blockedRequest.setRemoteAddr(clientIp);
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

            filter.doFilter(blockedRequest, blockedResponse, chain);

            assertThat(blockedResponse.getStatus()).isEqualTo(429);
            assertThat(blockedResponse.getContentType()).isEqualTo("application/problem+json;charset=UTF-8");
            assertThat(blockedResponse.getHeader("Retry-After")).isNotNull();

            long retryAfter = Long.parseLong(blockedResponse.getHeader("Retry-After"));
            assertThat(retryAfter).isGreaterThanOrEqualTo(1L);

            String body = blockedResponse.getContentAsString();
            assertThat(body).contains("\"type\":\"urn:problem-type:rate-limit-exceeded\"");
            assertThat(body).contains("\"title\":\"Too Many Requests\"");
            assertThat(body).contains("\"status\":429");
            assertThat(body).contains("\"instance\":\"/api/v1/workorders\"");

            // Xác nhận chuỗi lọc chỉ được gọi đúng 60 lần, không gọi lần thứ 61
            verify(chain, times(60)).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Chính sách giới hạn tần suất thao tác ghi (POST, PATCH)")
    class WritePolicyTests {

        @Test
        @DisplayName("Cho phép tối đa 20 yêu cầu POST mỗi phút cho cùng một địa chỉ IP")
        void postRequests_withinLimit_allPass() throws ServletException, IOException {
            String clientIp = "192.168.2.100";

            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/workorders");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                FilterChain chain = mock(FilterChain.class);

                filter.doFilter(request, response, chain);

                assertThat(response.getStatus()).isEqualTo(200);
                verify(chain, times(1)).doFilter(request, response);
            }
        }

        @Test
        @DisplayName("Từ chối yêu cầu POST thứ 21 với HTTP 429 Too Many Requests")
        void postRequests_exceedingLimit_returns429() throws ServletException, IOException {
            String clientIp = "192.168.2.101";
            FilterChain chain = mock(FilterChain.class);

            // Dùng hết 20 thẻ token
            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/workorders");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, chain);
            }

            // Yêu cầu thứ 21 phải bị chặn
            MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/api/v1/workorders");
            blockedRequest.setRemoteAddr(clientIp);
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

            filter.doFilter(blockedRequest, blockedResponse, chain);

            assertThat(blockedResponse.getStatus()).isEqualTo(429);
            assertThat(blockedResponse.getHeader("Retry-After")).isNotNull();
            assertThat(blockedResponse.getContentAsString()).contains("urn:problem-type:rate-limit-exceeded");
            verify(chain, times(20)).doFilter(any(), any());
        }

        @Test
        @DisplayName("Áp dụng giới hạn 20 yêu cầu/phút cho các yêu cầu cập nhật trạng thái PATCH")
        void patchRequests_enforcesWriteCapacity() throws ServletException, IOException {
            String clientIp = "192.168.2.102";
            FilterChain chain = mock(FilterChain.class);

            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/v1/workorders/123/status");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, chain);
            }

            MockHttpServletRequest blockedRequest = new MockHttpServletRequest("PATCH", "/api/v1/workorders/123/status");
            blockedRequest.setRemoteAddr(clientIp);
            MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

            filter.doFilter(blockedRequest, blockedResponse, chain);

            assertThat(blockedResponse.getStatus()).isEqualTo(429);
            verify(chain, times(20)).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Phân giải địa chỉ IP và cách ly máy khách")
    class ClientIpResolutionTests {

        @Test
        @DisplayName("Trích xuất chính xác địa chỉ IP từ tiêu đề X-Forwarded-For khi có mặt")
        void resolveClientIp_fromXForwardedFor() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
            request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("Sử dụng unknown-client khi remoteAddr có giá trị null hoặc rỗng")
        void resolveClientIp_fallbackToUnknownClient() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
            request.setRemoteAddr("");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(chain).doFilter(request, response);
        }

        @Test
        @DisplayName("Cách ly hoàn toàn các thùng thẻ giữa các địa chỉ IP máy khách khác nhau")
        void clientIpIsolation_independentBuckets() throws ServletException, IOException {
            String clientA = "10.0.0.1";
            String clientB = "10.0.0.2";
            FilterChain chain = mock(FilterChain.class);

            // Máy khách A tiêu thụ hết 60 token đọc
            for (int i = 0; i < 60; i++) {
                MockHttpServletRequest requestA = new MockHttpServletRequest("GET", "/api/v1/workorders");
                requestA.setRemoteAddr(clientA);
                MockHttpServletResponse responseA = new MockHttpServletResponse();
                filter.doFilter(requestA, responseA, chain);
            }

            // Yêu cầu thứ 61 của máy khách A bị chặn
            MockHttpServletRequest blockedA = new MockHttpServletRequest("GET", "/api/v1/workorders");
            blockedA.setRemoteAddr(clientA);
            MockHttpServletResponse responseA = new MockHttpServletResponse();
            filter.doFilter(blockedA, responseA, chain);
            assertThat(responseA.getStatus()).isEqualTo(429);

            // Máy khách B gửi yêu cầu GET thành công (sở hữu thùng 60 token độc lập)
            MockHttpServletRequest requestB = new MockHttpServletRequest("GET", "/api/v1/workorders");
            requestB.setRemoteAddr(clientB);
            MockHttpServletResponse responseB = new MockHttpServletResponse();
            filter.doFilter(requestB, responseB, chain);
            assertThat(responseB.getStatus()).isEqualTo(200);
        }
    }
}
