// Nguồn gốc AI: sinh từ docs/02-api-spec.md, docs/02-security-auth-spec.md, docs/00-internal-coding-standards.md
package com.gpc.oms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpc.oms.config.RoleConstants;
import com.gpc.oms.controller.WorkOrderController;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.exception.ProblemTypes;
import com.gpc.oms.testutil.WorkOrderTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bộ kiểm thử tích hợp đầu-cuối (End-to-End Integration Tests) xác thực toàn diện vòng đời phiếu công tác.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@DisplayName("Kiểm thử tích hợp đầu-cuối vòng đời Outage Work Order (E2E Integration Tests)")
class WorkOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Kịch bản 1: Luồng thành công vòng đời phiếu sự cố (Happy Path E2E)")
    class HappyPathLifecycleTests {

        @Test
        @DisplayName("Vòng đời hoàn chỉnh: Tạo mới (DISPATCHER) -> Xem danh sách -> Chi tiết (TECHNICIAN) -> Đang thi công -> Hoàn tất")
        void fullWorkOrderLifecycle() throws Exception {
            // Bước 1: Điều độ viên (DISPATCHER) tiếp nhận và tạo mới một phiếu sự cố
            String createJson = """
                {
                    "equipmentId": "TR-500KV-HANOI",
                    "description": "Máy biến áp 500kV trạm biến áp Đông Anh cảnh báo quá tải nhiệt",
                    "priority": "CRITICAL"
                }
                """;

            MvcResult createResult = mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("dispatcher1").roles(RoleConstants.DISPATCHER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.equipmentId").value("TR-500KV-HANOI"))
                .andExpect(jsonPath("$.status").value(WorkOrderStatus.OPEN.getValue()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andReturn();

            String responseContent = createResult.getResponse().getContentAsString();
            JsonNode rootNode = objectMapper.readTree(responseContent);
            String workOrderId = rootNode.get("id").asText();
            assertThat(createResult.getResponse().getHeader("Location"))
                .isEqualTo(WorkOrderController.PATH_WORKORDERS + "/" + workOrderId);

            // Bước 2: Điều độ viên gọi GET /api/v1/workorders có phân trang
            mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("dispatcher1").roles(RoleConstants.DISPATCHER))
                    .param("page", "0")
                    .param("size", "10")
                    .param("status", WorkOrderStatus.OPEN.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.id == '" + workOrderId + "')].equipmentId").value("TR-500KV-HANOI"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));

            // Bước 3: Kỹ thuật viên (TECHNICIAN) gọi GET /api/v1/workorders/{id} xem chi tiết
            mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS + "/{id}", workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles(RoleConstants.TECHNICIAN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.equipmentId").value("TR-500KV-HANOI"))
                .andExpect(jsonPath("$.status").value(WorkOrderStatus.OPEN.getValue()));

            // Bước 4: Kỹ thuật viên cập nhật trạng thái sang Đang thi công (InProgress)
            mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles(RoleConstants.TECHNICIAN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.IN_PROGRESS.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value(WorkOrderStatus.IN_PROGRESS.getValue()))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

            // Bước 5: Kỹ thuật viên cập nhật trạng thái sang Hoàn tất (Done)
            mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles(RoleConstants.TECHNICIAN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.DONE.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value(WorkOrderStatus.DONE.getValue()))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Kịch bản 2: Bảo mật & Ranh giới phân quyền RBAC (Security & RBAC Boundary)")
    class SecurityAndRbacBoundaryTests {

        @Test
        @DisplayName("6. Gọi API khi thiếu token xác thực trả về 401 Unauthorized (urn:problem-type:unauthorized)")
        void unauthenticatedRequest_returns401ProblemDetail() throws Exception {
            mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value(ProblemTypes.UNAUTHORIZED.toString()))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
        }

        @Test
        @DisplayName("7. DISPATCHER cố ý gọi PATCH cập nhật trạng thái bị từ chối 403 Forbidden")
        @WithMockUser(username = "dispatcher_user", roles = {RoleConstants.DISPATCHER})
        void dispatcherCannotPatchStatus_returns403ProblemDetail() throws Exception {
            final UUID randomId = UUID.randomUUID();

            mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, randomId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.IN_PROGRESS.getValue())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value(ProblemTypes.FORBIDDEN.toString()))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access Denied"));
        }
    }

    @Nested
    @DisplayName("Kịch bản 3: Xác thực đầu vào & Bất biến máy trạng thái (Validation & State Invariant)")
    class ValidationAndStateInvariantTests {

        @Test
        @DisplayName("8. POST thiếu equipmentId và description < 10 ký tự trả về 400 (validation-error)")
        @WithMockUser(username = "dispatcher_user", roles = {RoleConstants.DISPATCHER})
        void validationError_returns400ProblemDetail() throws Exception {
            final String invalidBody = """
                {
                    "equipmentId": "",
                    "description": "Short",
                    "priority": "HIGH"
                }
                """;

            mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION_ERROR.toString()))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value(ProblemTypes.TITLE_VALIDATION_FAILED))
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        @DisplayName("9. POST chứa thuộc tính lạ hoặc enum không hợp lệ trả về 400 (malformed-json)")
        @WithMockUser(username = "dispatcher_user", roles = {RoleConstants.DISPATCHER})
        void malformedJson_withUnknownPropertyOrInvalidEnum_returns400ProblemDetail() throws Exception {
            // Trường hợp 9a: Giá trị Enum không hợp lệ
            final String invalidEnumBody = """
                {
                    "equipmentId": "EQ-VALID-01",
                    "description": "Valid description with over 10 chars",
                    "priority": "NON_EXISTING_LEVEL"
                }
                """;

            mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidEnumBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value(ProblemTypes.MALFORMED_JSON.toString()))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value(ProblemTypes.TITLE_MALFORMED_REQUEST_BODY))
                .andExpect(jsonPath("$.invalidParams[0].name").value(ProblemTypes.FIELD_BODY));

            // Trường hợp 9b: Thuộc tính lạ bị từ chối bởi fail-on-unknown-properties: true
            final String unknownPropertyBody = """
                {
                    "equipmentId": "EQ-VALID-01",
                    "description": "Valid description with over 10 chars",
                    "priority": "HIGH",
                    "unrecognizedField": "ForbiddenValue"
                }
                """;

            mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unknownPropertyBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value(ProblemTypes.MALFORMED_JSON.toString()))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value(ProblemTypes.TITLE_MALFORMED_REQUEST_BODY));
        }

        @Test
        @DisplayName("10. GET với UUID ngẫu nhiên không tồn tại trả về 404 Not Found (not-found)")
        @WithMockUser(username = "tech_user", roles = {RoleConstants.TECHNICIAN})
        void getWithNonExistingUuid_returns404ProblemDetail() throws Exception {
            final UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS + "/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND.toString()))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("WorkOrder not found with id: " + nonExistentId));
        }

        @Test
        @DisplayName("11. Nhảy cóc trạng thái từ Open trực tiếp sang Done trả về 422 (invalid-state-transition)")
        void skipStateTransition_openToDone_returns422ProblemDetail() throws Exception {
            // Tạo mới một WorkOrder (bắt đầu từ trạng thái OPEN)
            final String createJson = """
                {
                    "equipmentId": "EQ-SKIP-TEST",
                    "description": "Kiểm tra nhảy cóc trạng thái từ Open sang Done",
                    "priority": "LOW"
                }
                """;

            final MvcResult createResult = mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("dispatcher1").roles(RoleConstants.DISPATCHER))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

            final String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            // Cố ý nhảy cóc trạng thái OPEN -> DONE
            mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, id)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles(RoleConstants.TECHNICIAN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.DONE.getValue())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value(ProblemTypes.INVALID_STATE_TRANSITION.toString()))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.detail").value("Invalid state transition from OPEN to DONE"));
        }
    }
}
