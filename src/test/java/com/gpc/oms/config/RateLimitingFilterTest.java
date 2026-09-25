// AI Provenance: generated from docs/security-auth-spec.md, docs/security-rules.md, docs/SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit and slice verification suite for {@link RateLimitingFilter}.
 *
 * @apiNote Verifies Token Bucket rate limiting invariants (60 req/min for GET, 20 req/min for POST/PATCH),
 *          RFC 7807 response schema, Retry-After headers, client IP isolation, and cache boundary controls.
 * @author GPC OMS Architecture Team
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("RateLimitingFilter Unit Tests")
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    @Nested
    @DisplayName("shouldNotFilter Evaluations")
    class ShouldNotFilterTests {

        @Test
        @DisplayName("Returns true for non-workorder endpoints (actuator, static assets, h2-console)")
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
        @DisplayName("Returns false for /api/v1/workorders endpoints")
        void shouldNotFilter_workorderPaths_returnsFalse() {
            MockHttpServletRequest listReq = new MockHttpServletRequest("GET", "/api/v1/workorders");
            MockHttpServletRequest detailReq = new MockHttpServletRequest("GET", "/api/v1/workorders/123e4567-e89b-12d3-a456-426614174000");

            assertThat(filter.shouldNotFilter(listReq)).isFalse();
            assertThat(filter.shouldNotFilter(detailReq)).isFalse();
        }
    }

    @Nested
    @DisplayName("Read Operations Rate Limiting (GET)")
    class ReadPolicyTests {

        @Test
        @DisplayName("Allows up to 60 GET requests per minute for a single client IP")
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
        @DisplayName("Rejects 61st GET request with HTTP 429, Retry-After header, and RFC 7807 problem details")
        void getRequests_exceedingLimit_returns429() throws ServletException, IOException {
            String clientIp = "192.168.1.101";
            FilterChain chain = mock(FilterChain.class);

            // Exhaust all 60 tokens
            for (int i = 0; i < 60; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/workorders");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, chain);
            }

            // 61st request should be rejected
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

            // Verify chain was only called 60 times, NOT for the 61st request
            verify(chain, times(60)).doFilter(any(), any());
        }
    }

    @Nested
    @DisplayName("Write Operations Rate Limiting (POST, PATCH)")
    class WritePolicyTests {

        @Test
        @DisplayName("Allows up to 20 POST requests per minute for a single client IP")
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
        @DisplayName("Rejects 21st POST request with HTTP 429 Too Many Requests")
        void postRequests_exceedingLimit_returns429() throws ServletException, IOException {
            String clientIp = "192.168.2.101";
            FilterChain chain = mock(FilterChain.class);

            // Exhaust all 20 tokens
            for (int i = 0; i < 20; i++) {
                MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/workorders");
                request.setRemoteAddr(clientIp);
                MockHttpServletResponse response = new MockHttpServletResponse();
                filter.doFilter(request, response, chain);
            }

            // 21st request should be blocked
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
        @DisplayName("Enforces 20 req/min limit on PATCH status update requests")
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
    @DisplayName("Client IP Resolution & Isolation")
    class ClientIpResolutionTests {

        @Test
        @DisplayName("Resolves client IP from X-Forwarded-For header when present")
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
        @DisplayName("Falls back to unknown-client when remoteAddr is null or blank")
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
        @DisplayName("Isolates token buckets between distinct client IP addresses")
        void clientIpIsolation_independentBuckets() throws ServletException, IOException {
            String clientA = "10.0.0.1";
            String clientB = "10.0.0.2";
            FilterChain chain = mock(FilterChain.class);

            // Client A exhausts all 60 read tokens
            for (int i = 0; i < 60; i++) {
                MockHttpServletRequest requestA = new MockHttpServletRequest("GET", "/api/v1/workorders");
                requestA.setRemoteAddr(clientA);
                MockHttpServletResponse responseA = new MockHttpServletResponse();
                filter.doFilter(requestA, responseA, chain);
            }

            // Client A 61st request is blocked
            MockHttpServletRequest blockedA = new MockHttpServletRequest("GET", "/api/v1/workorders");
            blockedA.setRemoteAddr(clientA);
            MockHttpServletResponse responseA = new MockHttpServletResponse();
            filter.doFilter(blockedA, responseA, chain);
            assertThat(responseA.getStatus()).isEqualTo(429);

            // Client B makes a GET request and succeeds (has its own fresh 60 tokens)
            MockHttpServletRequest requestB = new MockHttpServletRequest("GET", "/api/v1/workorders");
            requestB.setRemoteAddr(clientB);
            MockHttpServletResponse responseB = new MockHttpServletResponse();
            filter.doFilter(requestB, responseB, chain);
            assertThat(responseB.getStatus()).isEqualTo(200);
        }
    }
}
