// AI Provenance: generated from docs/api-spec.md, docs/security-auth-spec.md, docs/internal-coding-standards.md
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
@DisplayName("WorkOrder End-to-End Integration Tests")
class WorkOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Kịch bản 1: Happy Path Vòng Đời Phiếu Sự Cố (Lifecycle End-to-End)")
    class HappyPathLifecycleTests {

        @Test
        @DisplayName("Complete WorkOrder Lifecycle: Create (DISPATCHER) -> List -> Detail (TECHNICIAN) -> InProgress -> Done")
        void fullWorkOrderLifecycle() throws Exception {
            // Step 1: DISPATCHER creates a new work order
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

            // Step 2: DISPATCHER calls GET /api/v1/workorders with pagination
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

            // Step 3: TECHNICIAN calls GET /api/v1/workorders/{id}
            mockMvc.perform(get("/api/v1/workorders/{id}", workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles("TECHNICIAN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.equipmentId").value("TR-500KV-HANOI"))
                .andExpect(jsonPath("$.status").value("Open"));

            // Step 4: TECHNICIAN updates status to InProgress
            mockMvc.perform(patch("/api/v1/workorders/{id}/status", workOrderId)
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("tech1").roles("TECHNICIAN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\": \"InProgress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workOrderId))
                .andExpect(jsonPath("$.status").value("InProgress"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

            // Step 5: TECHNICIAN updates status to Done
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
    @DisplayName("Kịch bản 2: Bảo mật & RBAC Boundary")
    class SecurityAndRbacBoundaryTests {

        @Test
        @DisplayName("6. Calling API without authentication token returns 401 Unauthorized (urn:problem-type:unauthorized)")
        void unauthenticatedRequest_returns401ProblemDetail() throws Exception {
            mockMvc.perform(get("/api/v1/workorders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.detail").isNotEmpty());
        }

        @Test
        @DisplayName("7. DISPATCHER intentionally attempting to PATCH status returns 403 Forbidden (urn:problem-type:forbidden)")
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
    @DisplayName("Kịch bản 3: Validation & State Invariant")
    class ValidationAndStateInvariantTests {

        @Test
        @DisplayName("8. POST with missing equipmentId and description < 10 chars returns 400 (urn:problem-type:validation-error)")
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
        @DisplayName("9. POST with unknown properties or invalid enum returns 400 (urn:problem-type:malformed-json)")
        @WithMockUser(username = "dispatcher_user", roles = {"DISPATCHER"})
        void malformedJson_withUnknownPropertyOrInvalidEnum_returns400ProblemDetail() throws Exception {
            // Case 9a: Invalid Enum value
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

            // Case 9b: Unknown property with fail-on-unknown-properties: true
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
        @DisplayName("10. GET with random non-existing UUID returns 404 Not Found (urn:problem-type:not-found)")
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
        @DisplayName("11. Skipping state from Open directly to Done returns 422 (urn:problem-type:invalid-state-transition)")
        void skipStateTransition_openToDone_returns422ProblemDetail() throws Exception {
            // First create a new WorkOrder (starts at Open)
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

            // Attempt to transition OPEN -> DONE directly
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
