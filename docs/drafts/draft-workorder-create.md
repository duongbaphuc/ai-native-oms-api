<!--
Role: Senior Engineer. Task: POST /api/v1/workorders tạo WorkOrder.
Context files: docs/00-coding-rules.md, docs/00-api-rules.md, docs/00-security-rules.md.
Constraints: schema đúng docs/02-api-spec.md (equipmentId, priority), lỗi RFC 7807, @Valid, JPA only.
Architecture: 3-tier — Controller delegate sang WorkOrderService.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: POST /api/v1/workorders

## Target Files

| File | Path | Action |
|---|---|---|
| `WorkOrderController` | `src/main/java/com/gpc/oms/controller/WorkOrderController.java` | NEW (class declaration + POST method) |
| `WorkOrderService` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | Xem `draft-workorder-service.md` |

## Step-by-step Logic

### Controller Layer (`WorkOrderController.create`)
1. Annotation: `@PostMapping` + `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")`
2. Nhận `@Valid @RequestBody WorkOrderRequest req` — Spring tự validate trước khi vào method
3. Log: `log.info("create workorder equipmentIdHash={}", req.equipmentId().hashCode())` — KHÔNG log raw equipmentId (PII policy, xem `00-security-rules.md §4`)
4. Delegate: gọi `workOrderService.createWorkOrder(req)` — KHÔNG chứa business logic trong Controller
5. Return `ResponseEntity.created(URI.create("/api/v1/workorders/" + response.id())).body(response)`

### Service Layer (`WorkOrderService.createWorkOrder`)
→ Xem chi tiết tại [`draft-workorder-service.md`](draft-workorder-service.md) §Method 1.

## Error Mapping

| Điều kiện vi phạm | HTTP Status | RFC 7807 `type` | Message / Detail |
|---|---|---|---|
| Body thiếu field bắt buộc (`@NotBlank`, `@NotNull`) | 400 | `urn:problem-type:validation-error` | `invalidParams` chứa field name + reason |
| Body có field lạ (`ignoreUnknown=false`) | 400 | `urn:problem-type:malformed-json` | `Malformed Request Body` |
| Enum value không hợp lệ (vd: `"URGENT"`) | 400 | `urn:problem-type:malformed-json` | `invalidParams[].name=body` |
| Không có auth hoặc role không đủ | 403 | `urn:problem-type:forbidden` | `Access Denied` |
| Lỗi hệ thống không mong đợi | 500 | `urn:problem-type:internal-error` | `An unexpected error occurred` |

## Architectural Constraint

> [!IMPORTANT]
> Controller **KHÔNG chứa business logic**. Luồng: validate (`@Valid`) → authorize (`@PreAuthorize`) → delegate (`service.createWorkOrder`) → return.

## Controller Code

```java
// AI Provenance: generated from docs/02-api-spec.md §1, docs/00-coding-rules.md, docs/00-security-rules.md
package com.gpc.oms.controller;

import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.service.WorkOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workorders")
public class WorkOrderController {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderController.class);
    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<WorkOrderResponse> createWorkOrder(@Valid @RequestBody final WorkOrderRequest request) {
        log.info("create workorder equipmentId={}", request.equipmentId().hashCode());
        final WorkOrderResponse response = workOrderService.createWorkOrder(request);
        final URI location = URI.create("/api/v1/workorders/" + response.id());
        return ResponseEntity.created(location).body(response);
    }
}
```

## Checklist

- [ ] Controller inject `WorkOrderService` (không `WorkOrderRepository`)
- [ ] `@Valid` trên `@RequestBody`
- [ ] `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")`
- [ ] `HttpStatus.CREATED` (không dùng magic number `201`)
- [ ] Log hash của equipmentId, KHÔNG log raw value
- [ ] Business logic nằm trong Service, KHÔNG trong Controller
