<!--
Role: Senior Engineer. Task: Xây dựng 2 API GET /api/v1/workorders và GET /api/v1/workorders/{id}.
Context files: docs/api-spec.md, docs/security-rules.md
Constraints: 
- Lấy danh sách (GET /): Phân quyền DISPATCHER, trả về List<WorkOrderResponse>.
- Lấy chi tiết (GET /{id}): Phân quyền DISPATCHER hoặc TECHNICIAN, trả về WorkOrderResponse, nếu không tìm thấy trả lỗi 404 (ném ResponseStatusException để @ControllerAdvice bắt).
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: GET /api/v1/workorders

```java
    @GetMapping
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<List<WorkOrderResponse>> getAll() {
        log.info("get all workorders");
        List<WorkOrderResponse> list = repo.findAll().stream()
                .map(WorkOrderResponse::from)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TECHNICIAN') or hasRole('DISPATCHER')")
    public ResponseEntity<WorkOrderResponse> getById(@PathVariable UUID id) {
        log.info("get workorder by id={}", id);
        WorkOrder wo = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Work Order Not Found"));
        return ResponseEntity.ok(WorkOrderResponse.from(wo));
    }
```
