<!--
Role: Senior Engineer. Task: Xây dựng 2 API GET /api/v1/workorders và GET /api/v1/workorders/{id}.
Context files: docs/api-spec.md, docs/security-rules.md, docs/coding-rules.md
Constraints: 
- Lấy danh sách (GET /): Phân quyền DISPATCHER, trả về List<WorkOrderResponse>.
- Lấy chi tiết (GET /{id}): Phân quyền DISPATCHER hoặc TECHNICIAN, trả về WorkOrderResponse, nếu không tìm thấy trả lỗi 404 RFC 7807.
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

## Endpoint 1: `GET /api/v1/workorders` (List All)

### Step-by-step Logic

**Controller (`getAll`):**
1. Annotation: `@GetMapping` + `@PreAuthorize("hasRole('DISPATCHER')")`
2. Log: `log.info("get all workorders")`
3. Delegate: gọi `workOrderService.getAllWorkOrders()`
4. Return `ResponseEntity.ok(list)`

**Service (`getAllWorkOrders`):**  
→ Xem chi tiết tại [`draft-workorder-service.md`](draft-workorder-service.md) §Method 2.

> [!WARNING]
> **Open Question — Pagination:** `findAll()` không có pagination. Nếu table > 10K rows, cần thêm `Pageable` param + `Page<WorkOrderResponse>` return type. Quyết định cần xác nhận với PO về scale dự kiến.

### Error Mapping

| Điều kiện | HTTP | RFC 7807 `type` | Message |
|---|---|---|---|
| Role không phải DISPATCHER | 403 | `https://api.oms.gpc.com/errors/forbidden` | `Access Denied` |

### Controller Code

```java
    @GetMapping
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<List<WorkOrderResponse>> getAll() {
        log.info("get all workorders");
        List<WorkOrderResponse> list = workOrderService.getAllWorkOrders();
        return ResponseEntity.ok(list);
    }
```

---

## Endpoint 2: `GET /api/v1/workorders/{id}` (Get By ID)

### Step-by-step Logic

**Controller (`getById`):**
1. Annotation: `@GetMapping("/{id}")` + `@PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")`
2. Nhận `@PathVariable UUID id`
3. Log: `log.info("get workorder by id={}", id)` — UUID không phải PII, an toàn để log
4. Delegate: gọi `workOrderService.getWorkOrderById(id)`
5. Return `ResponseEntity.ok(response)`

**Service (`getWorkOrderById`):**  
→ Xem chi tiết tại [`draft-workorder-service.md`](draft-workorder-service.md) §Method 3.

### Error Mapping

| Điều kiện | HTTP | RFC 7807 `type` | Message |
|---|---|---|---|
| ID không tồn tại trong DB | 404 | `https://api.oms.gpc.com/errors/not-found` | `Work Order Not Found` |
| Role không đủ quyền | 403 | `https://api.oms.gpc.com/errors/forbidden` | `Access Denied` |
| ID format không phải UUID hợp lệ | 400 | `https://api.oms.gpc.com/errors/validation` | `Invalid UUID format` (Spring auto-reject) |

### Controller Code

```java
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")
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
- [ ] `getAll()`: `@PreAuthorize("hasRole('DISPATCHER')")` — chỉ DISPATCHER
- [ ] `getById()`: `@PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")` — cả 2 role
- [ ] Log không chứa PII
- [ ] Business logic (findAll, findById, error throw) nằm trong Service
