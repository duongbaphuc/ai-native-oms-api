<!--
Role: Senior Engineer. Task: Draft test case cho WorkOrder API (4 endpoint + RBAC + RFC 7807).
Context files: docs/domain-model.md, docs/api-spec.md, docs/coding-rules.md, docs/api-rules.md, docs/security-rules.md.
Constraints: JUnit 5 + MockMvc, schema đúng api-spec, lỗi assert theo RFC 7807 (type/title/status/invalidParams), không log PII.
Research: Context7 /spring-projects/spring-boot (@WebMvcTest addFilters=true tự apply security chain; Boot 4 dùng @MockitoBean);
Tavily Spring Boot 3.3 @WebMvcTest + @WithMockUser + @PreAuthorize pattern (docs.spring.io, Boot 3.4 release notes).
Stack note: repo dùng Boot 3.3.4 (Framework 6.1) nên @MockitoBean CHƯA tồn tại -> draft dùng @MockBean; khi lên Boot 3.4+ migrate sang @MockitoBean.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Test case WorkOrder API

## Acceptance matrix (Given/When/Then)

| # | Endpoint | Given | When | Then |
|---|---|---|---|---|
| 1 | POST /api/v1/workorders | role TECHNICIAN, body hợp lệ | create | 201 + đúng schema (id, equipmentId, description, priority, status Open, resolvedAt null) |
| 2 | POST | role DISPATCHER, body hợp lệ | create | 201 |
| 3 | POST | không auth / role lạ | create | 403 + RFC 7807 forbidden |
| 4 | POST | priority = URGENT | create | 400 + invalidParams[0].name=priority |
| 5 | POST | thiếu equipmentId | create | 400 + invalidParams chứa equipmentId |
| 6 | GET /api/v1/workorders | role DISPATCHER | list | 200 mảng đúng schema |
| 7 | GET /api/v1/workorders | role TECHNICIAN | list | 403 |
| 8 | GET /{id} | tồn tại, role TECHNICIAN | get | 200 object đơn |
| 9 | GET /{id} | id không tồn tại | get | 404 + RFC 7807 not-found |
| 10 | PATCH /{id}/status | Open -> InProgress | patch | 200 status InProgress, resolvedAt null |
| 11 | PATCH /{id}/status | InProgress -> Done | patch | 200 status Done, resolvedAt != null |
| 12 | PATCH /{id}/status | Open -> Done (nhảy cóc) | patch | 422 invalid-state-transition |
| 13 | PATCH /{id}/status | Done -> InProgress (lùi) | patch | 422 invalid-state-transition |
| 14 | PATCH /{id}/status | id không tồn tại | patch | 404 |
| 15 | Entity unit | Open.advanceStatus(Done) trực tiếp | gọi | throw IllegalStateException (không qua HTTP) |

## Sketch 1: unit test state machine (không cần Spring context)

```java
class WorkOrderTest {
    @Test
    void advanceStatus_allowsLinearFlow_andSetsResolvedAtOnDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        wo.advanceStatus(Status.InProgress);
        wo.advanceStatus(Status.Done);
        assertEquals(Status.Done, wo.getStatus());
        assertNotNull(wo.getResolvedAt());
    }

    @Test
    void advanceStatus_rejectsSkipAndRollback() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(Status.Done));
        wo.advanceStatus(Status.InProgress);
        wo.advanceStatus(Status.Done);
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(Status.InProgress));
    }
}
```

## Sketch 2: @WebMvcTest controller (slice MVC + security filters mặc định)

```java
@WebMvcTest(WorkOrderController.class)
// Nếu @EnableMethodSecurity nằm ở config app không load trong slice thì thêm:
// @Import(MethodSecurityConfig.class)
class WorkOrderControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @MockBean WorkOrderRepository repo; // Boot 3.3.4: @MockBean đúng; Boot 3.4+ -> @MockitoBean

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_validBody_returns201() throws Exception {
        WorkOrder saved = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        when(repo.save(any())).thenReturn(saved);
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải\",\"priority\":\"HIGH\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("Open"))
            .andExpect(jsonPath("$.resolvedAt").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void create_invalidPriority_returns400Rfc7807() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"equipmentId\":\"EQ-77\",\"description\":\"Quá tải\",\"priority\":\"URGENT\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://api.oms.gpc.com/errors/validation"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.invalidParams[0].name").value("priority"));
    }

    @Test
    void create_noAuth_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/workorders")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void list_technicianRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/workorders")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void patch_skipTransition_returns422() throws Exception {
        UUID id = UUID.randomUUID();
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH); // status Open
        when(repo.findById(id)).thenReturn(Optional.of(wo));
        mockMvc.perform(patch("/api/v1/workorders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"Done\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("https://api.oms.gpc.com/errors/invalid-state-transition"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void getById_missing_returns404() throws Exception {
        when(repo.findById(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/v1/workorders/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("https://api.oms.gpc.com/errors/not-found"));
    }
}
```

## Ghi chú review (không assert bằng test được)

- Không log PII: kiểm bằng review code, không viết test đọc log.
- `@WithMockUser(roles=...)` tự prefix `ROLE_`, khớp `hasRole` trong spec.
- Strict schema (`ignoreUnknown=false`) nên thêm 1 case field lạ -> 400 khi entity chốt.
