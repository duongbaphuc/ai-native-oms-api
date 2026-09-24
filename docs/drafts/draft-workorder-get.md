<!--
Role: Senior Engineer. Task: Xây dựng 2 API GET /api/v1/workorders và GET /api/v1/workorders/{id}.
Context files: docs/02-api-spec.md, docs/00-security-rules.md, docs/00-coding-rules.md
Constraints: 
- Lấy danh sách (GET /): Phân quyền DISPATCHER, TECHNICIAN, ADMIN, hỗ trợ phân trang (Pageable) và lọc status, trả về PagedResponse<WorkOrderResponse>.
- Lấy chi tiết (GET /{id}): Phân quyền DISPATCHER, TECHNICIAN, ADMIN, trả về WorkOrderResponse, nếu không tìm thấy trả lỗi 404 RFC 7807.
Architecture: 3-tier — Controller delegate sang WorkOrderService.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: GET /api/v1/workorders

## Target Files

| File | Path | Action |
|---|---|---|
| `WorkOrderController` | `src/main/java/com/gpc/oms/controller/WorkOrderController.java` | MODIFY (thêm 2 methods) |
| `WorkOrderService` | `src/main/java/com/gpc/oms/service/WorkOrderService.java` | Xem `draft-workorder-service.md` |

---

## Endpoint 1: `GET /api/v1/workorders` (Paged & Filtered List)

### Step-by-step Logic

**Controller (`getWorkOrders`):**
1. Annotation: `@GetMapping` + `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")`
2. Nhận tham số: `@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable` và `@RequestParam(required = false) WorkOrderStatus status`
3. Log: `log.info("get workorders page={} size={} status={}", pageable.getPageNumber(), pageable.getPageSize(), status)`
4. Delegate: gọi `workOrderService.getWorkOrders(pageable, status)`
5. Return `ResponseEntity.ok(response)`

**Service (`getWorkOrders`):**  
→ Xem chi tiết tại [`draft-workorder-service.md`](draft-workorder-service.md) §Method 2. Tuân thủ chuẩn `PagedResponse<T>` tại [`00-internal-coding-standards.md`](../00-internal-coding-standards.md) §3.

### Error Mapping

| Điều kiện | HTTP | RFC 7807 `type` | Message |
|---|---|---|---|
| Role không được phép (vd: GUEST) | 403 | `urn:problem-type:forbidden` | `Access Denied` |

### Controller Code

```java
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<PagedResponse<WorkOrderResponse>> getWorkOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) WorkOrderStatus status) {
        log.info("get workorders page={} size={} status={}", pageable.getPageNumber(), pageable.getPageSize(), status);
        PagedResponse<WorkOrderResponse> response = workOrderService.getWorkOrders(pageable, status);
        return ResponseEntity.ok(response);
    }
```

---

## Endpoint 2: `GET /api/v1/workorders/{id}` (Get By ID)

### Step-by-step Logic

**Controller (`getById`):**
1. Annotation: `@GetMapping("/{id}")` + `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")`
2. Nhận `@PathVariable UUID id`
3. Log: `log.info("get workorder by id={}", id)` — UUID không phải PII, an toàn để log
4. Delegate: gọi `workOrderService.getWorkOrderById(id)`
5. Return `ResponseEntity.ok(response)`

**Service (`getWorkOrderById`):**  
→ Xem chi tiết tại [`draft-workorder-service.md`](draft-workorder-service.md) §Method 3.

### Error Mapping

| Điều kiện | HTTP | RFC 7807 `type` | Message |
|---|---|---|---|
| ID không tồn tại trong DB | 404 | `urn:problem-type:not-found` | `WorkOrder not found with id: {id}` |
| Role không đủ quyền | 403 | `urn:problem-type:forbidden` | `Access Denied` |
| ID format không phải UUID hợp lệ | 400 | `urn:problem-type:type-mismatch` | `Parameter 'id' must be a valid UUID` |

### Controller Code

```java
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<WorkOrderResponse> getById(@PathVariable UUID id) {
        log.info("get workorder by id={}", id);
        WorkOrderResponse response = workOrderService.getWorkOrderById(id);
        return ResponseEntity.ok(response);
    }
```

---

## Architectural Constraint

> [!IMPORTANT]
> Controller **KHÔNG gọi Repository trực tiếp**. Mọi data access phải qua `WorkOrderService`. Controller chỉ: authorize → log → delegate → return.

## Checklist

- [ ] Controller inject `WorkOrderService` (không `WorkOrderRepository`)
- [ ] `getAll()`: `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` — cả 3 role
- [ ] `getById()`: `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` — cả 3 role
- [ ] Log không chứa PII
- [ ] Business logic (findAll, findById, error throw) nằm trong Service
