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
| DB error | `GlobalExceptionHandler` (fallback 500) | 500 |

---

## Method 2: `getAllWorkOrders() → List<WorkOrderResponse>`

### Step-by-step Logic
1. Gọi `repo.findAll()`
2. Map mỗi entity sang DTO: `.stream().map(WorkOrderResponse::from).toList()`
3. Return list

> [!WARNING]
> `findAll()` không có pagination. Nếu table > 10K rows, cần thêm `Pageable` param. Quyết định cần xác nhận với PO.

---

## Method 3: `getWorkOrderById(UUID id) → WorkOrderResponse`

### Step-by-step Logic
1. Gọi `repo.findById(id)`
2. Nếu `Optional.empty()` → `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found")`
3. Convert entity sang DTO: `WorkOrderResponse.from(entity)`
4. Return DTO

### Error Mapping
| Điều kiện | HTTP | RFC 7807 type |
|---|---|---|
| ID không tồn tại | 404 | `.../errors/not-found` |

---

## Method 4: `updateStatus(UUID id, WorkOrderStatusRequest req) → WorkOrderResponse`

### Step-by-step Logic
1. Gọi `repo.findById(id)`
2. Nếu `Optional.empty()` → `throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found")`
3. Gọi `entity.advanceStatus(req.status())` — delegate state machine check sang Entity
4. Nếu `IllegalStateException` bị throw → catch và wrap thành `ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage())`
5. Persist updated entity: `repo.save(entity)`
6. Convert sang DTO: `WorkOrderResponse.from(saved)`
7. Return DTO

### Error Mapping
| Điều kiện | HTTP | RFC 7807 type |
|---|---|---|
| ID không tồn tại | 404 | `.../errors/not-found` |
| State transition vi phạm (skip/rollback) | 422 | `.../errors/invalid-state-transition` |

---

## Service Code

```java
// AI Provenance: generated from docs/coding-rules.md, docs/api-rules.md, docs/domain-model.md
package com.gpc.oms.service;

import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderRepository;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class WorkOrderService {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);
    private final WorkOrderRepository repo;

    public WorkOrderService(WorkOrderRepository repo) {
        this.repo = repo;
    }

    public WorkOrderResponse createWorkOrder(WorkOrderRequest req) {
        WorkOrder entity = new WorkOrder(req.equipmentId(), req.description(), req.priority());
        WorkOrder saved = repo.save(entity);
        log.info("created workorder id={}", saved.getId());
        return WorkOrderResponse.from(saved);
    }

    public List<WorkOrderResponse> getAllWorkOrders() {
        return repo.findAll().stream()
                .map(WorkOrderResponse::from)
                .toList();
    }

    public WorkOrderResponse getWorkOrderById(UUID id) {
        WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found"));
        return WorkOrderResponse.from(entity);
    }

    public WorkOrderResponse updateStatus(UUID id, WorkOrderStatusRequest req) {
        WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found"));

        try {
            entity.advanceStatus(req.status());
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        }

        WorkOrder saved = repo.save(entity);
        log.info("updated workorder id={} status={}", saved.getId(), saved.getStatus());
        return WorkOrderResponse.from(saved);
    }
}
```

## Checklist

- [ ] Constructor Injection (không `@Autowired` trên field)
- [ ] `@Service` annotation
- [ ] Log chỉ `id` + `status`, KHÔNG log PII (equipmentId raw, description)
- [ ] State machine logic delegate sang `entity.advanceStatus()`, KHÔNG duplicate trong Service
- [ ] `ResponseStatusException` cho 404, 422 — để `GlobalExceptionHandler` map thành RFC 7807
- [ ] Return `WorkOrderResponse` DTO, KHÔNG return Entity
