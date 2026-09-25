// AI Provenance: generated from docs/02-api-spec.md, docs/00-security-rules.md, docs/drafts/draft-workorder-tests.md
package com.gpc.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpc.oms.config.RoleConstants;
import com.gpc.oms.config.SecurityConfig;
import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.PagedResponse;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.exception.GlobalExceptionHandler;
import com.gpc.oms.exception.ProblemTypes;
import com.gpc.oms.exception.ResourceNotFoundException;
import com.gpc.oms.service.WorkOrderService;
import com.gpc.oms.testutil.WorkOrderTestFixtures;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void create_validBody_returns201() throws Exception {
        final UUID id = UUID.randomUUID();
        final WorkOrderResponse response = WorkOrderTestFixtures.createResponse(
            id, "EQ-77", "Quá tải máy biến áp", Priority.HIGH,
            WorkOrderStatus.OPEN, Instant.now(), null);
        when(workOrderService.createWorkOrder(any())).thenReturn(response);

        mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(WorkOrderTestFixtures.createRequestJson("EQ-77", "Quá tải máy biến áp", "HIGH")))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", WorkOrderController.PATH_WORKORDERS + "/" + id))
            .andExpect(jsonPath("$.status").value(WorkOrderStatus.OPEN.getValue()))
            .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    // Row 3: No auth → 401
    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    // Row 4: Invalid enum value → 400 (HttpMessageNotReadableException, handler #2)
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void create_invalidPriority_returns400WithBodyParam() throws Exception {
        mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(WorkOrderTestFixtures.createRequestJson("EQ-77", "Quá tải máy biến áp", "URGENT")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemTypes.MALFORMED_JSON.toString()))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.invalidParams[0]." + ProblemTypes.KEY_NAME).value(ProblemTypes.FIELD_BODY));
    }

    // Row 5: Missing required field → 400 (MethodArgumentNotValidException, handler #1)
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void create_missingEquipmentId_returns400WithFieldParam() throws Exception {
        mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"description":"Quá tải máy biến áp","priority":"HIGH"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION_ERROR.toString()))
            .andExpect(jsonPath("$.invalidParams[0]." + ProblemTypes.KEY_NAME).value("equipmentId"));
    }

    // Row 6: Extra field (ignoreUnknown=false) → 400 (HttpMessageNotReadableException, handler #2)
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void create_extraField_returns400() throws Exception {
        mockMvc.perform(post(WorkOrderController.PATH_WORKORDERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"equipmentId":"EQ-77","description":"Quá tải máy biến áp","priority":"HIGH","status":"Open"}"""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemTypes.MALFORMED_JSON.toString()));
    }

    // === GET /api/v1/workorders ===

    // Row 7: DISPATCHER can list → 200
    @Test
    @WithMockUser(roles = RoleConstants.DISPATCHER)
    void list_dispatcherRole_returns200() throws Exception {
        when(workOrderService.getWorkOrders(any(), any()))
            .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(20));
    }

    // Row 8: TECHNICIAN can also list → 200
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void list_technicianRole_returns200() throws Exception {
        when(workOrderService.getWorkOrders(any(), any()))
            .thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0, true, true));

        mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    // === GET /api/v1/workorders/{id} ===

    // Row 9: Existing ID → 200
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void getById_existing_returns200() throws Exception {
        final UUID id = UUID.randomUUID();
        final WorkOrderResponse response = WorkOrderTestFixtures.createResponse(
            id, "EQ-77", "Quá tải máy biến áp", Priority.HIGH,
            WorkOrderStatus.OPEN, Instant.now(), null);
        when(workOrderService.getWorkOrderById(id)).thenReturn(response);

        mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS + "/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value(WorkOrderStatus.OPEN.getValue()));
    }

    // Row 10: Non-existing ID → 404
    @Test
    @WithMockUser(roles = RoleConstants.DISPATCHER)
    void getById_missing_returns404() throws Exception {
        final UUID id = UUID.randomUUID();
        when(workOrderService.getWorkOrderById(id))
            .thenThrow(ResourceNotFoundException.forWorkOrder(id));

        mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS + "/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value(ProblemTypes.NOT_FOUND.toString()));
    }

    // === PATCH /api/v1/workorders/{id}/status ===

    // Row 11: OPEN → IN_PROGRESS → 200 (TECHNICIAN can update status)
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void patch_validTransition_returns200() throws Exception {
        final UUID id = UUID.randomUUID();
        final WorkOrderResponse response = WorkOrderTestFixtures.createResponse(
            id, "EQ-77", "Quá tải máy biến áp", Priority.HIGH,
            WorkOrderStatus.IN_PROGRESS, Instant.now(), null);
        when(workOrderService.updateStatus(any(), any())).thenReturn(response);

        mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.IN_PROGRESS.getValue())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(WorkOrderStatus.IN_PROGRESS.getValue()))
            .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    // Row 13: Skip transition OPEN → DONE → 422
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void patch_skipTransition_returns422() throws Exception {
        final UUID id = UUID.randomUUID();
        when(workOrderService.updateStatus(any(), any()))
            .thenThrow(new IllegalStateException("Invalid state transition from OPEN to DONE"));

        mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.DONE.getValue())))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value(ProblemTypes.INVALID_STATE_TRANSITION.toString()));
    }

    // RBAC: DISPATCHER is Forbidden to patch status → 403
    @Test
    @WithMockUser(roles = RoleConstants.DISPATCHER)
    void patch_dispatcherRole_returns403Forbidden() throws Exception {
        final UUID id = UUID.randomUUID();
        mockMvc.perform(patch(WorkOrderController.PATH_WORKORDERS + WorkOrderController.PATH_STATUS, id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(WorkOrderTestFixtures.createStatusRequestJson(WorkOrderStatus.IN_PROGRESS.getValue())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type").value(ProblemTypes.FORBIDDEN.toString()));
    }

    // Invalid ?status= query value → 400 (F-01: was fallback 500)
    @Test
    @WithMockUser(roles = RoleConstants.DISPATCHER)
    void list_invalidStatusQueryParam_returns400() throws Exception {
        mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS)
                .param("status", "URGENT"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value(ProblemTypes.VALIDATION_ERROR.toString()))
            .andExpect(jsonPath("$.invalidParams[0]." + ProblemTypes.KEY_NAME).value("status"));
    }

    // Row 15: Unexpected Exception fallback → 500
    @Test
    @WithMockUser(roles = RoleConstants.TECHNICIAN)
    void getById_unexpectedException_returns500() throws Exception {
        final UUID id = UUID.randomUUID();
        when(workOrderService.getWorkOrderById(id))
            .thenThrow(new RuntimeException("Database connection timeout"));

        mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS + "/{id}", id))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value(ProblemTypes.INTERNAL_ERROR.toString()))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value(ProblemTypes.DETAIL_INTERNAL_ERROR));
    }

    // SEC-02: ?size= vượt trần bị cap về max-page-size=100 (chống DoS OOM)
    @Test
    @WithMockUser(roles = RoleConstants.DISPATCHER)
    void list_sizeOverMax_isCappedTo100() throws Exception {
        final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(workOrderService.getWorkOrders(pageableCaptor.capture(), any()))
            .thenReturn(new PagedResponse<>(List.of(), 0, 100, 0, 0, true, true));

        mockMvc.perform(get(WorkOrderController.PATH_WORKORDERS)
                .param("size", "200"))
            .andExpect(status().isOk());

        assertEquals(100, pageableCaptor.getValue().getPageSize());
    }
}
