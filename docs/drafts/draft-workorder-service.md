<!--
Role: Senior Engineer. Task: Define WorkOrderService — trung gian giữa Controller và Repository.
Context files: docs/coding-rules.md, docs/api-rules.md, docs/domain-model.md, docs/security-rules.md
Constraints:
  - 3-tier architecture: Controller → Service → Repository.
  - Service chứa orchestration logic (convert DTO ↔ Entity, gọi repo, handle exception).
  - Service KHÔNG chứa invariant logic (state machine nằm trong Entity).
  - Constructor Injection, KHÔNG dùng @Autowired trên field.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: WorkOrderService

## Target File
| File | Path | Action |
|---|---|---|
| `WorkOrderService` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | NEW |

**Package:** `com.gpc.oms.service`

## Architectural Constraint

```
Controller (@Valid, @PreAuthorize)
    ↓ delegate (DTO in, DTO out)
Service (orchestration: DTO→Entity, repo call, Entity→DTO)
    ↓ persist
Repository (JPA only)
    ↓ invariant check
Entity (state machine, domain rules)
```

- Controller: validate input + authorize + delegate. **Không chứa business logic.**
- Service: orchestrate. Convert DTO → Entity, gọi repo, convert Entity → DTO.
- Entity: chứa invariant (state machine `advanceStatus`). **Không phụ thuộc Spring.**
- Repository: persistence only.

---

## Method 1: `createWorkOrder(WorkOrderRequest req) → WorkOrderResponse`

### Step-by-step Logic
1. Tạo entity mới: `new WorkOrder(req.equipmentId(), req.description(), req.priority())`
2. Persist entity: `WorkOrder saved = repo.save(entity)`
3. Convert sang DTO: `WorkOrderResponse.from(saved)`
4. Return DTO

### Error Mapping (xử lý bởi layer khác)
| Điều kiện | Xử lý bởi | HTTP |
|---|---|---|
| `@Valid` fail | Controller + `GlobalExceptionHandler` | 400 |
| Auth fail | Spring Security + `GlobalExceptionHandler` | 403 |
| ID not found | Service throws `ResourceNotFoundException` → `GlobalExceptionHandler` | 404 |
| DB error | `GlobalExceptionHandler` (fallback 500) | 500 |

---

## Method 2: `getWorkOrders(Pageable pageable, WorkOrderStatus status) → PagedResponse<WorkOrderResponse>`

### Step-by-step Logic
1. Kiểm tra tham số lọc trạng thái:
   - Nếu `status != null` → gọi `repo.findByStatus(status, pageable)`
   - Nếu `status == null` → gọi `repo.findAll(pageable)`
2. Ánh xạ từng entity sang DTO qua `page.map(WorkOrderResponse::from)`
3. Đóng gói kết quả phân trang qua `PagedResponse.from(pageDto)`
4. Return `PagedResponse<WorkOrderResponse>`

---

## Method 3: `getWorkOrderById(UUID id) → WorkOrderResponse`

### Step-by-step Logic
1. Gọi `repo.findById(id)`
2. Nếu `Optional.empty()` → `throw new ResourceNotFoundException("WorkOrder not found with id: " + id)`
3. Convert entity sang DTO: `WorkOrderResponse.from(entity)`
4. Return DTO

### Error Mapping
| Điều kiện | HTTP | RFC 7807 type |
|---|---|---|
| ID không tồn tại | 404 | `urn:problem-type:not-found` |

---

## Method 4: `updateStatus(UUID id, WorkOrderStatusRequest req) → WorkOrderResponse`

### Step-by-step Logic
1. Gọi `repo.findById(id)`
2. Nếu `Optional.empty()` → `throw new ResourceNotFoundException("WorkOrder not found with id: " + id)`
3. Gọi `entity.advanceStatus(req.status())` — delegate state machine check sang Entity
4. Nếu `IllegalStateException` bị throw → re-throw trực tiếp để `GlobalExceptionHandler` map thành HTTP 422
5. Persist updated entity: `repo.save(entity)`
6. Convert sang DTO: `WorkOrderResponse.from(saved)`
7. Return DTO

### Error Mapping
| Condition | HTTP | RFC 7807 type |
|---|---|---|
| ID không tồn tại | 404 | `urn:problem-type:not-found` |
| State transition vi phạm (skip/rollback) | 422 | `urn:problem-type:invalid-state-transition` |

---

## Service Code

```java
// AI Provenance: generated from docs/coding-rules.md, docs/api-rules.md, docs/domain-model.md, docs/internal-coding-standards.md
package com.gpc.oms.service;

import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderRepository;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.PagedResponse;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import com.gpc.oms.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkOrderService {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);
    private final WorkOrderRepository repo;

    public WorkOrderService(WorkOrderRepository repo) {
        this.repo = repo;
    }

    public WorkOrderResponse createWorkOrder(final WorkOrderRequest req) {
        final WorkOrder entity = new WorkOrder(req.equipmentId(), req.description(), req.priority());
        final WorkOrder saved = repo.save(entity);
        log.info("created workorder id={}", saved.getId());
        return WorkOrderResponse.from(saved);
    }

    public PagedResponse<WorkOrderResponse> getWorkOrders(final Pageable pageable, final WorkOrderStatus status) {
        final Page<WorkOrder> page = (status != null)
                ? repo.findByStatus(status, pageable)
                : repo.findAll(pageable);
        return PagedResponse.from(page.map(WorkOrderResponse::from));
    }

    public WorkOrderResponse getWorkOrderById(final UUID id) {
        final WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));
        return WorkOrderResponse.from(entity);
    }

    public WorkOrderResponse updateStatus(final UUID id, final WorkOrderStatusRequest req) {
        final WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));

        try {
            entity.advanceStatus(req.status());
        } catch (IllegalStateException ex) {
            throw ex; // Re-throw — GlobalExceptionHandler sẽ map thành 422
        }

        final WorkOrder saved = repo.save(entity);
        log.info("updated workorder id={} status={}", saved.getId(), saved.getStatus());
        return WorkOrderResponse.from(saved);
    }
}
```

## Checklist

- [ ] Constructor Injection (không `@Autowired` trên field)
- [ ] `@Service` annotation
- [ ] Log chỉ `id` + `status`, KHÔNG log PII (equipmentId raw, description)
- [ ] Phân trang với `PagedResponse<WorkOrderResponse>` và hỗ trợ lọc `status`
- [ ] State machine logic delegate sang `entity.advanceStatus()`, KHÔNG duplicate trong Service
- [ ] `ResourceNotFoundException` cho 404 — để `GlobalExceptionHandler` map thành RFC 7807
- [ ] `IllegalStateException` cho 422 — re-throw, `GlobalExceptionHandler` map thành RFC 7807
- [ ] Return DTO (`WorkOrderResponse` / `PagedResponse`), KHÔNG return Entity
- [ ] Oracle Senior Java Style: Sử dụng `final` cho parameters và immutable local variables để tối ưu JIT Escape Analysis.
