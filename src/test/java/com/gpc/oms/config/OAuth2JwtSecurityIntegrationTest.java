// Nguồn gốc AI: sinh từ docs/02-security-auth-spec.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpc.oms.controller.WorkOrderController;
import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import com.gpc.oms.exception.GlobalExceptionHandler;
import com.gpc.oms.exception.ProblemTypes;
import com.gpc.oms.service.WorkOrderService;
import com.gpc.oms.testutil.WorkOrderTestFixtures;
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
 * Kiểm thử phân lớp bảo mật (Web Slice Security Tests) xác thực OAuth2 JWT và ranh giới phân quyền RBAC.
 *
 * <p>Kiểm tra quyền hạn của vai trò Dispatcher và Technician, thực thi ranh giới bảo vệ (403 Forbidden),
 * và xử lý các yêu cầu không xác thực (401 Unauthorized kèm RFC 7807 Problem Details).</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@WebMvcTest(WorkOrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("Kiểm thử tích hợp bảo mật OAuth2 JWT và RBAC")
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
    @DisplayName("Kiểm định Bean JwtAuthenticationConverter")
    class ConverterBeanTests {

        @Test
        @DisplayName("Bean JwtAuthenticationConverter trích xuất danh tính và vai trò chính xác")
        void jwtAuthenticationConverter_extractsPrincipalAndRoles() {
            final Jwt jwt = new Jwt(
                    "mock-token-string",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("alg", "none"),
                    Map.of("sub", "dispatcher-jane", "roles", List.of(RoleConstants.DISPATCHER))
            );

            final var auth = jwtAuthenticationConverter.convert(jwt);
            assertThat(auth).isNotNull();
            assertThat(auth.getName()).isEqualTo("dispatcher-jane");
            assertThat(auth.getAuthorities())
                    .extracting("authority")
                    .containsExactly(RoleConstants.ROLE_DISPATCHER);
        }
    }

    @Nested
    @DisplayName("Thực thi phân quyền RBAC qua OAuth2 JWT trên các endpoint WorkOrder")
    class RbacEnforcementTests {

        @Test
        @DisplayName("POST /api/v1/workorders với vai trò DISPATCHER thành công (201 Created)")
        void createWorkOrder_withDispatcherRole_returns201() throws Exception {
            final UUID id = UUID.randomUUID();
            final WorkOrderResponse response = WorkOrderTestFixtures.createResponse(
                    id, "EQ-001", "Transformer overheating",
                    Priority.CRITICAL, WorkOrderStatus.OPEN, Instant.now(), null
            );
            when(workOrderService.createWorkOrder(any())).thenReturn(response);

            final String requestBody = WorkOrderTestFixtures.createRequestJson(
                    "EQ-001", "Transformer overheating", "CRITICAL");

            mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                            .with(jwt().authorities(new SimpleGrantedAuthority(RoleConstants.ROLE_DISPATCHER)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value(WorkOrderStatus.OPEN.getValue()));
        }

        @Test
        @DisplayName("PATCH /api/v1/workorders/{id}/status với vai trò DISPATCHER bị từ chối (403 Forbidden)")
        void updateStatus_withDispatcherRole_returns403() throws Exception {
            final UUID id = UUID.randomUUID();
            final String requestBody = WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.IN_PROGRESS.getValue());

            mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, id)
                            .with(jwt().authorities(new SimpleGrantedAuthority(RoleConstants.ROLE_DISPATCHER)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value(ProblemTypes.FORBIDDEN.toString()))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @DisplayName("PATCH /api/v1/workorders/{id}/status với vai trò TECHNICIAN thành công (200 OK)")
        void updateStatus_withTechnicianRole_returns200() throws Exception {
            final UUID id = UUID.randomUUID();
            final WorkOrderResponse response = WorkOrderTestFixtures.createResponse(
                    id, "EQ-001", "Transformer overheating",
                    Priority.CRITICAL, WorkOrderStatus.IN_PROGRESS, Instant.now(), null
            );
            when(workOrderService.updateStatus(eq(id), any(WorkOrderStatusRequest.class))).thenReturn(response);

            final String requestBody = WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.IN_PROGRESS.getValue());

            mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, id)
                            .with(jwt().authorities(new SimpleGrantedAuthority(RoleConstants.ROLE_TECHNICIAN)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(WorkOrderStatus.IN_PROGRESS.getValue()));
        }

        @Test
        @DisplayName("POST /api/v1/workorders không có token xác thực trả về 401 Unauthorized Problem Details")
        void createWorkOrder_unauthenticated_returns401() throws Exception {
            final String requestBody = WorkOrderTestFixtures.createRequestJson(
                    "EQ-001", "Transformer overheating", "CRITICAL");

            mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value(ProblemTypes.UNAUTHORIZED.toString()))
                    .andExpect(jsonPath("$.status").value(401));
        }
    }
}
