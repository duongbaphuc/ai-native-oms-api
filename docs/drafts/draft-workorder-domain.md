<!--
Role: Senior Engineer. Task: Define WorkOrder entity, Enums, and Repository.
Context files: docs/domain-model.md, docs/coding-rules.md
Constraints: JPA Entity, UUID, Enum types. NO business logic leaking.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Work Order Domain

```java
@Entity
@Table(name = "work_orders")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String equipmentId;
    
    @Column(nullable = false)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status;
    
    @Column(nullable = false)
    private Instant createdAt;
    
    private Instant resolvedAt;

    protected WorkOrder() {} // JPA only

    public WorkOrder(String equipmentId, String description, Priority priority) {
        this.equipmentId = equipmentId;
        this.description = description;
        this.priority = priority;
        this.status = WorkOrderStatus.Open;
        this.createdAt = Instant.now();
    }
    
    // ... getters omitted for brevity in draft ...
    
    // Cập nhật trạng thái kèm quy tắc bất biến
    public void advanceStatus(WorkOrderStatus newStatus) {
        if (this.status == WorkOrderStatus.Done || newStatus == WorkOrderStatus.Open ||
           (this.status == WorkOrderStatus.InProgress && newStatus != WorkOrderStatus.Done)) {
            throw new IllegalStateException("Invalid State Transition");
        }
        this.status = newStatus;
        if (this.status == WorkOrderStatus.Done) {
            this.resolvedAt = Instant.now();
        }
    }
}

public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
public enum WorkOrderStatus { Open, InProgress, Done }

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {}
```
