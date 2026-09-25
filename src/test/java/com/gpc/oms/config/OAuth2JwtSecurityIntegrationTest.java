// AI Provenance: generated from docs/02-security-auth-spec.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpc.oms.controller.WorkOrderController;
import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import com.gpc.oms.exception.GlobalExceptionHandler;
import com.gpc.oms.service.WorkOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice security tests validating OAuth2 JWT token authentication and Method Security RBAC boundaries.
 *
 * @apiNote Verifies Dispatcher and Technician role permissions, RBAC boundary enforcement (403 Forbidden),
 *          and unauthenticated request handling (401 Unauthorized Problem Details).
 * @author GPC OMS Architecture Team
 * @version 1.0.0
 * @since 1.0.0
 */
@WebMvcTest(WorkOrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("OAuth2 JWT Security Integration Tests")
class OAuth2JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockBean
    private WorkOrderService workOrderService;

    @Nested
    @DisplayName("JWT Authentication Converter Bean Verification")
    class ConverterBeanTests {

        @Test
        @DisplayName("JwtAuthenticationConverter bean extracts authorities and principal name correctly")
        void jwtAuthenticationConverter_extractsPrincipalAndRoles() {
            Jwt jwt = new Jwt(
                    "mock-token-string",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("alg", "none"),
                    Map.of("sub", "dispatcher-jane", "roles", List.of("DISPATCHER"))
            );

            var auth = jwtAuthenticationConverter.convert(jwt);
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("dispatcher-jane");
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_DISPATCHER");
        }
    }

    @Nested
    @DisplayName("OAuth2 JWT RBAC Enforcement on WorkOrder Endpoints")
    class RbacEnforcementTests {

        @Test
        @DisplayName("POST /api/v1/workorders with DISPATCHER role succeeds (201 Created)")
        void createWorkOrder_withDispatcherRole_returns201() throws Exception {
            UUID id = UUID.randomUUID();
            WorkOrderResponse response = new WorkOrderResponse(
                    id, "EQ-001", "Transformer overheating",
                    Priority.CRITICAL, WorkOrderStatus.OPEN, Instant.now(), null
            );
            when(workOrderService.createWorkOrder(any())).thenReturn(response);

            String requestBody = """
                {
                    "equipmentId": "EQ-001",
                    "description": "Transformer overheating",
                    "priority": "CRITICAL"
                }
                """;

            mockMvc.perform(post("/api/v1/workorders")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value("Open"));
        }

        @Test
        @DisplayName("PATCH /api/v1/workorders/{id}/status with DISPATCHER role is forbidden (403 Forbidden)")
        void updateStatus_withDispatcherRole_returns403() throws Exception {
            UUID id = UUID.randomUUID();
            String requestBody = """
                {"status": "InProgress"}
                """;

            mockMvc.perform(patch("/api/v1/workorders/{id}/status", id)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("urn:problem-type:forbidden"))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("PATCH /api/v1/workorders/{id}/status with TECHNICIAN role succeeds (200 OK)")
        void updateStatus_withTechnicianRole_returns200() throws Exception {
            UUID id = UUID.randomUUID();
            WorkOrderResponse response = new WorkOrderResponse(
                    id, "EQ-001", "Transformer overheating",
                    Priority.CRITICAL, WorkOrderStatus.IN_PROGRESS, Instant.now(), null
            );
            when(workOrderService.updateStatus(eq(id), any(WorkOrderStatusRequest.class))).thenReturn(response);

            String requestBody = """
                {"status": "InProgress"}
                """;

            mockMvc.perform(patch("/api/v1/workorders/{id}/status", id)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("InProgress"));
        }

        @Test
        @DisplayName("POST /api/v1/workorders without token returns 401 Unauthorized Problem Details")
        void createWorkOrder_unauthenticated_returns401() throws Exception {
            String requestBody = """
                {
                    "equipmentId": "EQ-001",
                    "description": "Transformer overheating",
                    "priority": "CRITICAL"
                }
                """;

            mockMvc.perform(post("/api/v1/workorders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                    .andExpect(jsonPath("$.status").value(401));
        }
    }
}
