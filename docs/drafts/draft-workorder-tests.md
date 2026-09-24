<!--
Role: Senior Engineer. Task: Draft test case cho WorkOrder API (4 endpoint + RBAC + RFC 7807).
Context files: docs/domain-model.md, docs/api-spec.md, docs/coding-rules.md, docs/api-rules.md, docs/security-rules.md.
Constraints: JUnit 5 + MockMvc, schema đúng api-spec, lỗi assert theo RFC 7807 (type/title/status/invalidParams), không log PII.
Architecture: 3-tier — Controller test mock WorkOrderService (không mock Repository trực tiếp).
Research: Context7 /spring-projects/spring-boot (@WebMvcTest addFilters=true tự apply security chain; Boot 4 dùng @MockitoBean);
Tavily Spring Boot 3.3 @WebMvcTest + @WithMockUser + @PreAuthorize pattern (docs.spring.io, Boot 3.4 release notes).
Stack note: repo dùng Boot 3.3.4 (Framework 6.1) nên @MockitoBean CHƯA tồn tại -> draft dùng @MockBean; khi lên Boot 3.4+ migrate sang @MockitoBean.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Test case WorkOrder API

## Target Files

| File | Path | Action |
|---|---|---|
| `WorkOrderTest` | `src/test/java/com/gpc/oms/domain/WorkOrderTest.java` | NEW |
| `WorkOrderControllerTest` | `src/test/java/com/gpc/oms/controller/WorkOrderControllerTest.java` | NEW |

---

## Acceptance matrix (Given/When/Then)

| # | Endpoint | Given | When | Then |
|---|---|---|---|---|
| 1 | POST /api/v1/workorders | role TECHNICIAN, body hợp lệ | create | 201 + đúng schema (id, equipmentId, description, priority, status "Open", resolvedAt null) |
| 2 | POST | role DISPATCHER, body hợp lệ | create | 201 |
| 3 | POST | không auth | create | 401 + RFC 7807 unauthorized |
| 4 | POST | priority = "URGENT" (enum không hợp lệ) | create | 400 + `type=.../errors/validation`, `invalidParams[0].name=body` (handler: `handleMalformedJson`) |
| 5 | POST | thiếu equipmentId | create | 400 + `invalidParams[].name=equipmentId` (handler: `handleValidationErrors`) |
| 6 | POST | body có field lạ (`"status": "Open"`) | create | 400 + `type=.../errors/validation` (handler: `handleMalformedJson`) |
| 7 | GET /api/v1/workorders | role DISPATCHER | list | 200 PagedResponse đúng schema (content, pageNumber, pageSize, totalElements) |
| 8 | GET /api/v1/workorders | role TECHNICIAN | list | 200 PagedResponse đúng schema (content, pageNumber, pageSize, totalElements) |
| 9 | GET /{id} | tồn tại, role TECHNICIAN | get | 200 object đơn |
| 10 | GET /{id} | id không tồn tại | get | 404 + RFC 7807 `type=.../errors/not-found` |
| 11 | PATCH /{id}/status | OPEN → IN_PROGRESS | patch | 200 status "InProgress", resolvedAt null |
| 12 | PATCH /{id}/status | IN_PROGRESS → DONE | patch | 200 status "Done", resolvedAt != null |
| 13 | PATCH /{id}/status | OPEN → DONE (nhảy cóc) | patch | 422 `type=.../errors/invalid-state-transition` |
| 14 | PATCH /{id}/status | DONE → IN_PROGRESS (lùi) | patch | 422 `type=.../errors/invalid-state-transition` |
| 15 | PATCH /{id}/status | id không tồn tại | patch | 404 |
| 16 | Entity unit | OPEN.advanceStatus(DONE) trực tiếp | gọi | throw IllegalStateException (không qua HTTP) |

> [!IMPORTANT]
> **Row 4 vs Row 5:** Invalid enum value (`"URGENT"`) trigger `HttpMessageNotReadableException` (handler #2, assert `invalidParams[0].name=body`). Thiếu field trigger `MethodArgumentNotValidException` (handler #1, assert `invalidParams[0].name=equipmentId`). Hai handler khác nhau, hai assertion pattern khác nhau.

> [!IMPORTANT]
> **Row 6 (NEW):** Test `@JsonIgnoreProperties(ignoreUnknown = false)` — field lạ trong body phải bị reject 400.

---

## Sketch 1: Unit test state machine (không cần Spring context)

**Target file:** `src/test/java/com/gpc/oms/domain/WorkOrderTest.java`

```java
// AI Provenance: generated from docs/domain-model.md §Invariants
package com.gpc.oms.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkOrderTest {

    @Test
    void advanceStatus_allowsLinearFlow_andSetsResolvedAtOnDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        assertEquals(WorkOrderStatus.OPEN, wo.getStatus());
        assertNull(wo.getResolvedAt());

        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
        assertNull(wo.getResolvedAt());

        wo.advanceStatus(WorkOrderStatus.DONE);
        assertEquals(WorkOrderStatus.DONE, wo.getStatus());
        assertNotNull(wo.getResolvedAt());
    }

    @Test
    void advanceStatus_rejectsSkip_openToDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        // OPEN → DONE (skip IN_PROGRESS) — PHẢI bị cấm
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.DONE));
    }

    @Test
    void advanceStatus_rejectsRollback_doneToInProgress() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);
        // DONE → IN_PROGRESS (rollback) — PHẢI bị cấm
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.IN_PROGRESS));
    }

    @Test
    void advanceStatus_rejectsRollback_inProgressToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        // IN_PROGRESS → OPEN (rollback) — PHẢI bị cấm
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.OPEN));
    }

    @Test
    void constructor_setsDefaultValues() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        assertNotNull(wo.getCreatedAt());
        assertEquals(WorkOrderStatus.OPEN, wo.getStatus());
        assertNull(wo.getResolvedAt());
    }
}
```

---

## Sketch 2: @WebMvcTest controller (slice MVC + security filters mặc định)

**Target file:** `src/test/java/com/gpc/oms/controller/WorkOrderControllerTest.java`

> [!IMPORTANT]
> **3-tier architecture:** Test mock `WorkOrderService` (KHÔNG mock `WorkOrderRepository`). Controller chỉ delegate, nên test focus vào: HTTP status, request validation, authorization, response schema.

```java
// AI Provenance: generated from docs/api-spec.md, docs/security-rules.md
package com.gpc.oms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.service.WorkOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.gpc.oms.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Nếu @EnableMethodSecurity nằm ở config app không load trong slice thì thêm:
// @Import(MethodSecurityConfig.class)
@WebMvcTest(WorkOrderController.class)
class WorkOrderControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @MockBean WorkOrderService workOrderService; // 3-tier: mock Service, NOT Repository

    // === POST /api/v1/workorders ===

    // Row 1: Happy path — TECHNICIAN tạo work order
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_validBody_returns201() throws Exception {
        WorkOrderResponse response = new WorkOrderResponse(
            UUID.randomUUID(), "EQ-77", "Quá tải", Priority.HIGH,
            WorkOrderStatus.OPEN, Instant.now(), null);
        when(workOrderService.createWorkOrder(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải\",\"priority\":\"HIGH\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Open"))
            .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    // Row 3: No auth → 401 (Spring Security returns 401 when no authentication token provided)
    @Test
    void create_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
    }

    // Row 4: Invalid enum value → 400 (HttpMessageNotReadableException, NOT MethodArgumentNotValidException)
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_invalidPriority_returns400WithBodyParam() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải\",\"priority\":\"URGENT\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.invalidParams[0].name").value("body"));
        // NOTE: "URGENT" triggers HttpMessageNotReadableException (handler #2)
        // NOT MethodArgumentNotValidException (handler #1)
        // So invalidParams[0].name = "body", NOT "priority"
    }

    // Row 5: Missing required field → 400
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_missingEquipmentId_returns400WithFieldParam() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Quá tải\",\"priority\":\"HIGH\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("urn:problem-type:validation-error"))
            .andExpect(jsonPath("$.invalidParams[0].name").value("equipmentId"));
        // NOTE: Missing @NotBlank field triggers MethodArgumentNotValidException (handler #1)
        // invalidParams[0].name = "equipmentId"
    }

    // Row 6: Extra field (ignoreUnknown=false) → 400
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_extraField_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải\",\"priority\":\"HIGH\",\"status\":\"Open\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-json"));
        // NOTE: Extra field triggers HttpMessageNotReadableException (handler #2)
    }

    // === GET /api/v1/workorders ===

    // Row 7: DISPATCHER can list → 200
    @Test
    @WithMockUser(roles = "DISPATCHER")
    void list_dispatcherRole_returns200() throws Exception {
        when(workOrderService.getAllWorkOrders()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/workorders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // Row 8: TECHNICIAN can also list → 200
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void list_technicianRole_returns200() throws Exception {
        when(workOrderService.getAllWorkOrders()).thenReturn(List.of());
        mockMvc.perform(get("/api/v1/workorders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    // === GET /api/v1/workorders/{id} ===

    // Row 9: Existing ID → 200
    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getById_existing_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        WorkOrderResponse response = new WorkOrderResponse(
            id, "EQ-77", "Quá tải", Priority.HIGH,
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
            id, "EQ-77", "Quá tải", Priority.HIGH,
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
}
```

---

## Ghi chú Review (không assert bằng test được)

- Không log PII: kiểm bằng review code, không viết test đọc log.
- `@WithMockUser(roles=...)` tự prefix `ROLE_`, khớp `hasRole` trong spec.
- Boot 3.3.4: dùng `@MockBean`. Khi lên Boot 3.4+, migrate sang `@MockitoBean`.
- Test mock `WorkOrderService`, KHÔNG mock `WorkOrderRepository` — đúng 3-tier architecture.

## Checklist

- [ ] `@WebMvcTest(WorkOrderController.class)` — slice test
- [ ] `@MockBean WorkOrderService` (KHÔNG `WorkOrderRepository`)
- [ ] Test row 4: assert `invalidParams[0].name=body` (handler #2, NOT handler #1)
- [ ] Test row 6: extra field `ignoreUnknown=false` → 400
- [ ] Enum reference: `WorkOrderStatus.OPEN`, `WorkOrderStatus.IN_PROGRESS`, `WorkOrderStatus.DONE` (UPPER_SNAKE)
- [ ] State machine unit test: 4 cases (happy path, skip, rollback-from-done, rollback-from-in-progress)
- [ ] Tất cả test methods có `@WithMockUser` hoặc test absence of auth
