// AI Provenance: generated from docs/02-api-spec.md, docs/00-security-rules.md, docs/drafts/draft-workorder-tests.md
package com.gpc.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpc.oms.config.SecurityConfig;
import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.PagedResponse;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.exception.GlobalExceptionHandler;
import com.gpc.oms.exception.ResourceNotFoundException;
import com.gpc.oms.service.WorkOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Kiểm thử phân lớp Controller (WebMvcTest) cho {@link WorkOrderController}.
 *
 * <p>Kiểm tra các hành vi tiếp nhận request, phân tích cú pháp, ánh xạ tham số và xử lý mã trạng thái HTTP.</p>
 */
@WebMvcTest(WorkOrderController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("Kiểm thử phân lớp WebMvcTest cho WorkOrderController")
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private WorkOrderService workOrderService;

    // === POST /api/v1/workorders ===

    // Row 1: Happy path — TECHNICIAN tạo work order
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_validBody_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        WorkOrderResponse response = new WorkOrderResponse(
            id, "EQ-77", "Quá tải máy biến áp", Priority.HIGH,
            WorkOrderStatus.OPEN, Instant.now(), null);
        when(workOrderService.createWorkOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải máy biến áp\",\"priority\":\"HIGH\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/workorders/" + id))
            .andExpect(jsonPath("$.status").value("Open"))
            .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    // Row 3: No auth → 401
    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    // Row 4: Invalid enum value → 400 (HttpMessageNotReadableException, handler #2)
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_invalidPriority_returns400WithBodyParam() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải máy biến áp\",\"priority\":\"URGENT\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.invalidParams[0].name").value("body"));
    }

    // Row 5: Missing required field → 400 (MethodArgumentNotValidException, handler #1)
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_missingEquipmentId_returns400WithFieldParam() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Quá tải máy biến áp\",\"priority\":\"HIGH\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"))
            .andExpect(jsonPath("$.invalidParams[0].name").value("equipmentId"));
    }

    // Row 6: Extra field (ignoreUnknown=false) → 400 (HttpMessageNotReadableException, handler #2)
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_extraField_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải máy biến áp\",\"priority\":\"HIGH\",\"status\":\"Open\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"));
    }

    // === GET /api/v1/workorders ===

    // Row 7: DISPATCHER can list → 200
    @Test
    @WithMockUser(roles = "DISPATCHER")
    void list_dispatcherRole_returns200() throws Exception {
        when(workOrderService.getWorkOrders(any(), any()))
            .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/workorders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(20));
    }

    // Row 8: TECHNICIAN can also list → 200
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void list_technicianRole_returns200() throws Exception {
        when(workOrderService.getWorkOrders(any(), any()))
            .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/workorders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    // === GET /api/v1/workorders/{id} ===

    // Row 9: Existing ID → 200
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getById_existing_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        WorkOrderResponse response = new WorkOrderResponse(
            id, "EQ-77", "Quá tải máy biến áp", Priority.HIGH,
            WorkOrderStatus.OPEN, Instant.now(), null);
        when(workOrderService.getWorkOrderById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/workorders/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("Open"));
    }

    // Row 10: Non-existing ID → 404
    @Test
    @WithMockUser(roles = "DISPATCHER")
    void getById_missing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(workOrderService.getWorkOrderById(id))
            .thenThrow(new ResourceNotFoundException("WorkOrder not found with id: " + id));

        mockMvc.perform(get("/api/v1/workorders/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("urn:problem-type:not-found"));
    }

    // === PATCH /api/v1/workorders/{id}/status ===

    // Row 11: OPEN → IN_PROGRESS → 200 (TECHNICIAN can update status)
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void patch_validTransition_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        WorkOrderResponse response = new WorkOrderResponse(
            id, "EQ-77", "Quá tải máy biến áp", Priority.HIGH,
            WorkOrderStatus.IN_PROGRESS, Instant.now(), null);
        when(workOrderService.updateStatus(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/workorders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"InProgress\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("InProgress"))
            .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    // Row 13: Skip transition OPEN → DONE → 422
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void patch_skipTransition_returns422() throws Exception {
        UUID id = UUID.randomUUID();
        when(workOrderService.updateStatus(any(), any()))
            .thenThrow(new IllegalStateException("Invalid state transition from OPEN to DONE"));

        mockMvc.perform(patch("/api/v1/workorders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Done\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-state-transition"));
    }

    // RBAC: DISPATCHER is Forbidden to patch status → 403
    @Test
    @WithMockUser(roles = "DISPATCHER")
    void patch_dispatcherRole_returns403Forbidden() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/api/v1/workorders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"InProgress\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type").value("urn:problem-type:forbidden"));
    }

    // Invalid ?status= query value → 400 (F-01: was fallback 500)
    @Test
    @WithMockUser(roles = "DISPATCHER")
    void list_invalidStatusQueryParam_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/workorders")
                .param("status", "URGENT"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"))
            .andExpect(jsonPath("$.invalidParams[0].name").value("status"));
    }

    // Row 15: Unexpected Exception fallback → 500
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getById_unexpectedException_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(workOrderService.getWorkOrderById(id))
            .thenThrow(new RuntimeException("Database connection timeout"));

        mockMvc.perform(get("/api/v1/workorders/{id}", id))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value("urn:problem-type:internal-error"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    // SEC-02: ?size= vượt trần bị cap về max-page-size=100 (chống DoS OOM)
    @Test
    @WithMockUser(roles = "DISPATCHER")
    void list_sizeOverMax_isCappedTo100() throws Exception {
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(workOrderService.getWorkOrders(pageableCaptor.capture(), any()))
            .thenReturn(new PagedResponse<>(List.of(), 0, 100, 0, 0, true, true));

        mockMvc.perform(get("/api/v1/workorders")
                .param("size", "200"))
            .andExpect(status().isOk());

        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }
}
