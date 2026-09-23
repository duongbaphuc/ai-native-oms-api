<!--
Role: Senior Engineer. Task: POST /api/v1/workorders tạo WorkOrder.
Context files: docs/coding-rules.md, docs/api-rules.md, docs/security-rules.md.
Constraints: schema đúng docs/api-spec.md (equipmentId, priority), lỗi RFC 7807, @Valid, JPA only.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: POST /api/v1/workorders

```java
@RestController
@RequestMapping("/api/v1/workorders")
public class WorkOrderController {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderController.class);
    private final WorkOrderRepository repo;
    public WorkOrderController(WorkOrderRepository repo) { this.repo = repo; }

    @PostMapping
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<WorkOrderResponse> create(@Valid @RequestBody WorkOrderRequest req) {
        log.info("create workorder equipmentIdHash={}", req.equipmentId().hashCode());
        WorkOrder e = new WorkOrder(req.equipmentId(), req.priority());
        return ResponseEntity.status(201).body(WorkOrderResponse.from(repo.save(e)));
    }
}
```
