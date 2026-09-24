<!--
Role: Senior Engineer. Task: PATCH /api/v1/workorders/{id}/status
Context files: docs/01-domain-model.md, docs/02-api-spec.md, docs/00-api-rules.md, docs/00-coding-rules.md
Constraints: One-way state machine rule (strict linear), HTTP 422 mapping RFC 7807 on invalid state.
Architecture: 3-tier — Controller delegate sang WorkOrderService.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: PATCH /api/v1/workorders/{id}/status

## Target Files

| File | Path | Action |
|---|---|---|
| `WorkOrderController` | `src/main/java/com/gpc/oms/controller/WorkOrderController.java` | MODIFY (thêm method) |
| `WorkOrderService` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | Xem `draft-workorder-service.md` |

## Step-by-step Logic

### Controller Layer (`updateStatus`)
1. Annotation: `@PatchMapping("/{id}/status")` + `@PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")`
2. Nhận `@PathVariable UUID id` và `@Valid @RequestBody WorkOrderStatusRequest req`
3. Log: `log.info("update status workorderId={}", id)` — UUID không phải PII
4. Delegate: gọi `workOrderService.updateStatus(id, req)` — KHÔNG chứa business logic trong Controller
5. Return `ResponseEntity.ok(response)`

### Service Layer (`updateStatus`)
→ Xem chi tiết tại [`draft-workorder-service.md`](draft-workorder-service.md) §Method 4.

**Tóm tắt:** Service gọi `entity.advanceStatus(req.status())` → Entity delegate validation sang `WorkOrderStatus.canTransitionTo()` → nếu vi phạm, throw `IllegalStateException` → Service re-throw → `GlobalExceptionHandler` map thành **422 Unprocessable Entity** (RFC 7807).

## Error Mapping

| Điều kiện vi phạm | HTTP Status | RFC 7807 `type` | Message |
|---|---|---|---|
| ID không tồn tại | 404 | `urn:problem-type:not-found` | `WorkOrder not found with id: {id}` |
| State transition vi phạm (skip: OPEN→DONE, rollback: DONE→IN_PROGRESS) | 422 | `urn:problem-type:invalid-state-transition` | `Invalid state transition from {current} to {target}` |
| `status` field null | 400 | `urn:problem-type:validation-error` | `invalidParams[].name=status` |
| Enum value không hợp lệ (vd: `"PENDING"`) | 400 | `urn:problem-type:malformed-json` | `Malformed Request Body` |
| Body có field lạ (`ignoreUnknown=false`) | 400 | `urn:problem-type:malformed-json` | `Malformed Request Body` |
| Role không đủ quyền (vd: DISPATCHER) | 403 | `urn:problem-type:forbidden` | `Access Denied: Only TECHNICIAN or ADMIN can update status` |

## Request DTO

> [!IMPORTANT]
> Sử dụng `WorkOrderStatusRequest` (từ [`draft-dtos.md`](draft-dtos.md) §2.2), **KHÔNG** tạo class `StatusUpdateRequest` riêng. Tránh duplicate DTO.

## Architectural Constraint

```
Controller (@Valid, @PreAuthorize)
    ↓ delegate
Service (findById → advanceStatus → save)
    ↓ state machine check
Entity.advanceStatus() → WorkOrderStatus.canTransitionTo()
    ↓ nếu vi phạm
IllegalStateException → Service re-throw → GlobalExceptionHandler(422)
    ↓ GlobalExceptionHandler
RFC 7807 ProblemDetail JSON
```

## Controller Code

```java
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<WorkOrderResponse> updateStatus(
            @PathVariable UUID id, 
            @Valid @RequestBody WorkOrderStatusRequest req) {
        
        log.info("update status workorderId={}", id);
        WorkOrderResponse response = workOrderService.updateStatus(id, req);
        return ResponseEntity.ok(response);
    }
```

## Checklist

- [ ] Dùng `WorkOrderStatusRequest` (không phải `StatusUpdateRequest`)
- [ ] Controller inject `WorkOrderService` (không `WorkOrderRepository`)
- [ ] `@Valid` trên `@RequestBody` — validate `@NotNull` trước khi vào method
- [ ] `@PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")`
- [ ] State machine check: Entity → Enum, KHÔNG trong Controller hoặc Service
- [ ] 422 Unprocessable Entity cho invalid state transition (không dùng 400)
