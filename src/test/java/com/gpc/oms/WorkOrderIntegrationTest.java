// Nguồn gốc AI: sinh từ docs/02-api-spec.md, docs/02-security-auth-spec.md, docs/00-internal-coding-standards.md
package com.gpc.oms;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

            MvcResult createResult = mockMvc.perform(post("/api/v1/workorders")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("dispatcher1").roles("DISPATCHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.equipmentId").value("TR-500KV-HANOI"))
                .andExpect(jsonPath("$.status").value("Open"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andReturn();

            String responseContent = createResult.getResponse().getContentAsString();
            JsonNode rootNode = objectMapper.readTree(responseContent);
            String workOrderId = rootNode.get("id").asText();
            assertThat(createResult.getResponse().getHeader("Location"))
                .isEqualTo("/api/v1/workorders/" + workOrderId);

            // Bước 2: Điều độ viên gọi GET /api/v1/workorders có phân trang
            mockMvc.perform(get("/api/v1/workorders")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("dispatcher1").roles("DISPATCHER"))
                    .param("page", "0")
                    .param("size", "10")
                    .param("status", "Open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.id == '" + workOrderId + "')].equipmentId").value("TR-500KV-HANOI"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10));

            // Bước 3: Kỹ thuật viên (TECHNICIAN) gọi GET /api/v1/workorders/{id} xem chi tiết
            mockMvc.perform(get("/api/v1/workorders/{id}", workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles("TECHNICIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.equipmentId").value("TR-500KV-HANOI"))
                .andExpect(jsonPath("$.status").value("Open"));

            // Bước 4: Kỹ thuật viên cập nhật trạng thái sang Đang thi công (InProgress)
            mockMvc.perform(patch("/api/v1/workorders/{id}/status", workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles("TECHNICIAN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": \"InProgress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value("InProgress"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

            // Bước 5: Kỹ thuật viên cập nhật trạng thái sang Hoàn tất (Done)
            mockMvc.perform(patch("/api/v1/workorders/{id}/status", workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles("TECHNICIAN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": \"Done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value("Done"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("Kịch bản 2: Bảo mật & Ranh giới phân quyền RBAC (Security & RBAC Boundary)")
    class SecurityAndRbacBoundaryTests {

        @Test
        @DisplayName("6. Gọi API khi thiếu token xác thực trả về 401 Unauthorized (urn:problem-type:unauthorized)")
        void unauthenticatedRequest_returns401ProblemDetail() throws Exception {
            mockMvc.perform(get("/api/v1/workorders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
        }

        @Test
        @DisplayName("7. DISPATCHER cố ý gọi PATCH cập nhật trạng thái bị từ chối 403 Forbidden")
        @WithMockUser(username = "dispatcher_user", roles = {"DISPATCHER"})
        void dispatcherCannotPatchStatus_returns403ProblemDetail() throws Exception {
            UUID randomId = UUID.randomUUID();

            mockMvc.perform(patch("/api/v1/workorders/{id}/status", randomId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": \"InProgress\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.type").value("urn:problem-type:forbidden"))
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.detail").value("Access Denied"));
        }
    }

    @Nested
    @DisplayName("Kịch bản 3: Xác thực đầu vào & Bất biến máy trạng thái (Validation & State Invariant)")
    class ValidationAndStateInvariantTests {

        @Test
        @DisplayName("8. POST thiếu equipmentId và description < 10 ký tự trả về 400 (validation-error)")
        @WithMockUser(username = "dispatcher_user", roles = {"DISPATCHER"})
        void validationError_returns400ProblemDetail() throws Exception {
            String invalidBody = """
                {
                    "equipmentId": "",
                    "description": "Short",
                    "priority": "HIGH"
                }
                """;

            mockMvc.perform(post("/api/v1/workorders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams", hasSize(greaterThanOrEqualTo(2))));
        }

        @Test
        @DisplayName("9. POST chứa thuộc tính lạ hoặc enum không hợp lệ trả về 400 (malformed-json)")
        @WithMockUser(username = "dispatcher_user", roles = {"DISPATCHER"})
        void malformedJson_withUnknownPropertyOrInvalidEnum_returns400ProblemDetail() throws Exception {
            // Trường hợp 9a: Giá trị Enum không hợp lệ
            String invalidEnumBody = """
                {
                    "equipmentId": "EQ-VALID-01",
                    "description": "Valid description with over 10 chars",
                    "priority": "NON_EXISTING_LEVEL"
                }
                """;

            mockMvc.perform(post("/api/v1/workorders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidEnumBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Malformed Request Body"))
                .andExpect(jsonPath("$.invalidParams[0].name").value("body"));

            // Trường hợp 9b: Thuộc tính lạ bị từ chối bởi fail-on-unknown-properties: true
            String unknownPropertyBody = """
                {
                    "equipmentId": "EQ-VALID-01",
                    "description": "Valid description with over 10 chars",
                    "priority": "HIGH",
                    "unrecognizedField": "ForbiddenValue"
                }
                """;

            mockMvc.perform(post("/api/v1/workorders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(unknownPropertyBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Malformed Request Body"));
        }

        @Test
        @DisplayName("10. GET với UUID ngẫu nhiên không tồn tại trả về 404 Not Found (not-found)")
        @WithMockUser(username = "tech_user", roles = {"TECHNICIAN"})
        void getWithNonExistingUuid_returns404ProblemDetail() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/v1/workorders/{id}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("WorkOrder not found with id: " + nonExistentId));
        }

        @Test
        @DisplayName("11. Nhảy cóc trạng thái từ Open trực tiếp sang Done trả về 422 (invalid-state-transition)")
        void skipStateTransition_openToDone_returns422ProblemDetail() throws Exception {
            // Tạo mới một WorkOrder (bắt đầu từ trạng thái OPEN)
            String createJson = """
                {
                    "equipmentId": "EQ-SKIP-TEST",
                    "description": "Kiểm tra nhảy cóc trạng thái từ Open sang Done",
                    "priority": "LOW"
                }
                """;

            MvcResult createResult = mockMvc.perform(post("/api/v1/workorders")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("dispatcher1").roles("DISPATCHER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson))
                .andExpect(status().isCreated())
                .andReturn();

            String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

            // Cố ý nhảy cóc trạng thái OPEN -> DONE
            mockMvc.perform(patch("/api/v1/workorders/{id}/status", id)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles("TECHNICIAN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": \"Done\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-state-transition"))
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.detail").value("Invalid state transition from OPEN to DONE"));
        }
    }
}
