<!--
Role: Senior Engineer. Task: PATCH /api/v1/workorders/{id}/status
Context files: docs/domain-model.md, docs/api-spec.md, docs/api-rules.md, docs/coding-rules.md
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
1. Annotation: `@PatchMapping("/{id}/status")` + `@PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")`
2. Nhận `@PathVariable UUID id` và `@Valid @RequestBody WorkOrderStatusRequest req`
3. Log: `log.info("update status workorderId={}", id)` — UUID không phải PII
4. Delegate: gọi `workOrderService.updateStatus(id, req)` — KHÔNG chứa business logic trong Controller
5. Return `ResponseEntity.ok(response)`

### Service Layer (`updateStatus`)
→ Xem chi tiết tại [`draft-workorder-service.md`](draft-workorder-service.md) §Method 4.

**Tóm tắt:** Service gọi `entity.advanceStatus(req.status())` → Entity delegate validation sang `WorkOrderStatus.canTransitionTo()` → nếu vi phạm, throw `IllegalStateException` → Service catch và wrap thành `ResponseStatusException(422)`.

## Error Mapping

| Điều kiện vi phạm | HTTP Status | RFC 7807 `type` | Message |
|---|---|---|---|
| ID không tồn tại | 404 | `https://api.oms.gpc.com/errors/not-found` | `Work Order Not Found` |
| State transition vi phạm (skip: OPEN→DONE, rollback: DONE→IN_PROGRESS) | 422 | `https://api.oms.gpc.com/errors/invalid-state-transition` | `Invalid state transition from X to Y` |
| `status` field null | 400 | `https://api.oms.gpc.com/errors/validation` | `invalidParams[].name=status` |
| Enum value không hợp lệ (vd: `"PENDING"`) | 400 | `https://api.oms.gpc.com/errors/validation` | `Malformed Request Body` |
| Body có field lạ (`ignoreUnknown=false`) | 400 | `https://api.oms.gpc.com/errors/validation` | `Malformed Request Body` |
| Role không đủ quyền | 403 | `https://api.oms.gpc.com/errors/forbidden` | `Access Denied` |

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
IllegalStateException → Service catch → ResponseStatusException(422)
    ↓ GlobalExceptionHandler
RFC 7807 ProblemDetail JSON
```

## Controller Code

```java
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")
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
- [ ] `@PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")`
- [ ] State machine check: Entity → Enum, KHÔNG trong Controller hoặc Service
- [ ] 422 Unprocessable Entity cho invalid state transition (không dùng 400)
