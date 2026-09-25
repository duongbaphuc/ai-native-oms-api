<!--
Role: Senior Engineer. Task: Define WorkOrder entity, Enums, and Repository.
Context files: docs/01-domain-model.md, docs/00-coding-rules.md
Constraints: JPA Entity, UUID, Enum types. NO business logic leaking.
Architecture: 3-tier (Controller → Service → Repository). Entity chỉ chứa invariant logic.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Work Order Domain

## 1. Entity Schema — `WorkOrder`

**Target file:** `src/main/java/com/gpc/oms/domain/WorkOrder.java`  
**Package:** `com.gpc.oms.domain`

| Field | Type | Constraint | Description |
|---|---|---|---|
| `id` | `UUID` | `@Id @GeneratedValue(strategy = GenerationType.UUID)`, PK | Định danh hệ thống, auto-generated |
| `equipmentId` | `String` | `@Column(nullable = false, length = 50)` | Mã thiết bị lưới điện. Max 50 chars (khớp `WorkOrderRequest`) |
| `description` | `String` | `@Column(nullable = false, length = 500)` | Mô tả sự cố. Max 500 chars (khớp `WorkOrderRequest`) |
| `priority` | `Priority` | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` | Mức độ: LOW, MEDIUM, HIGH, CRITICAL |
| `status` | `WorkOrderStatus` | `@Enumerated(EnumType.STRING)`, `@Column(nullable = false)` | Trạng thái vòng đời (state machine) |
| `createdAt` | `Instant` | `@Column(nullable = false)`, gán trong constructor | Thời điểm ghi nhận sự cố |
| `resolvedAt` | `Instant` | nullable, auto-set khi `status = DONE` | Thời điểm khắc phục xong |

## 2. Constructor Logic (Step-by-step)

1. Nhận 3 tham số: `equipmentId`, `description`, `priority`
2. Gán `this.equipmentId = equipmentId`
3. Gán `this.description = description`
4. Gán `this.priority = priority`
5. Gán `this.status = WorkOrderStatus.OPEN` — mọi WorkOrder mới bắt đầu ở trạng thái OPEN
6. Gán `this.createdAt = Instant.now()`
7. `this.resolvedAt` giữ `null` — chỉ gán khi chuyển sang DONE

## 3. Method `advanceStatus(WorkOrderStatus newStatus)` — Pseudo-code

1. Gọi `this.status.canTransitionTo(newStatus)` — delegate validation sang Enum
2. Nếu `false` → `throw new IllegalStateException("Invalid state transition from " + this.status + " to " + newStatus)`
3. Nếu `true` → `this.status = newStatus`
4. Nếu `newStatus == WorkOrderStatus.DONE` → `this.resolvedAt = Instant.now()`

> [!IMPORTANT]
> `canTransitionTo()` trong Enum `WorkOrderStatus` là **source-of-truth** duy nhất cho state machine logic. Entity method `advanceStatus()` chỉ là orchestrator, KHÔNG chứa logic chuyển trạng thái.

## 4. Entity Code

```java
// AI Provenance: generated from docs/01-domain-model.md, docs/00-coding-rules.md
package com.gpc.oms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
public class WorkOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 50)
    private String equipmentId;
    
    @Column(nullable = false, length = 500)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderStatus status;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @Column(nullable = true)
    private Instant resolvedAt;

    protected WorkOrder() {} // JPA only — không gọi từ application code

    public WorkOrder(final String equipmentId, final String description, final Priority priority) {
        this.equipmentId = java.util.Objects.requireNonNull(equipmentId, "equipmentId must not be null");
        this.description = java.util.Objects.requireNonNull(description, "description must not be null");
        this.priority = java.util.Objects.requireNonNull(priority, "priority must not be null");
        this.status = WorkOrderStatus.OPEN;
        this.createdAt = Instant.now();
    }
    
    // --- Getters (manual, không dùng Lombok — theo 00-coding-rules.md) ---
    public UUID getId() { return id; }
    public String getEquipmentId() { return equipmentId; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    
    /**
     * Chuyển trạng thái theo quy tắc bất biến (one-way state machine).
     * Delegate validation sang WorkOrderStatus.canTransitionTo().
     */
    public void advanceStatus(final WorkOrderStatus newStatus) {
        java.util.Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Invalid state transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
        if (this.status == WorkOrderStatus.DONE) {
            this.resolvedAt = Instant.now();
        }
    }
}
```

## 5. Enums

### 5.1 `Priority` — `src/main/java/com/gpc/oms/domain/Priority.java`

Khai báo trong `01-domain-model.md` — 4 mức độ nghiêm trọng của sự cố.

```java
// AI Provenance: generated from docs/01-domain-model.md §Entities
package com.gpc.oms.domain;

public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
```

### 5.2 `WorkOrderStatus` — `src/main/java/com/gpc/oms/domain/WorkOrderStatus.java`

Khai báo trong `01-domain-model.md` — máy trạng thái đơn hướng (one-way state machine).  
`@JsonValue` serialize ra chuỗi khớp `02-api-spec.md` (ví dụ: `"Open"` thay vì `"OPEN"`).

**State Machine Rules (Strict Linear):**
1. `OPEN` → `IN_PROGRESS` ✅
2. `IN_PROGRESS` → `DONE` ✅
3. Mọi chuyển đổi khác (skip, rollback) → ❌ `throw IllegalStateException`

```java
// AI Provenance: generated from docs/01-domain-model.md §Invariants, docs/02-api-spec.md §4
package com.gpc.oms.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkOrderStatus {
    OPEN("Open"),
    IN_PROGRESS("InProgress"),
    DONE("Done");

    private final String value;

    WorkOrderStatus(final String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    /**
     * Kiểm tra tính hợp lệ của chuyển trạng thái.
     * Bắt buộc: OPEN → IN_PROGRESS → DONE. Lùi hoặc nhảy cóc bị cấm tuyệt đối.
     */
    public boolean canTransitionTo(final WorkOrderStatus next) {
        return switch (this) {
            case OPEN        -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == DONE;
            case DONE        -> false; // terminal — không có chuyển tiếp nào hợp lệ
        };
    }
}
```

## 6. Repository — `src/main/java/com/gpc/oms/domain/WorkOrderRepository.java`

```java
// AI Provenance: generated from docs/01-domain-model.md, docs/02-database-migration-spec.md §3
package com.gpc.oms.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    Page<WorkOrder> findByStatus(WorkOrderStatus status, Pageable pageable);
}
```

## 7. Checklist

- [ ] Entity dùng `@Table(name = "work_orders")`
- [ ] `@Column(length = 50)` cho `equipmentId`, `@Column(length = 500)` cho `description`
- [ ] Enum convention: UPPER_SNAKE nội bộ + `@JsonValue` cho JSON serialize
- [ ] `canTransitionTo()` là source-of-truth cho state machine (strict linear)
- [ ] `advanceStatus()` delegate sang `canTransitionTo()`, KHÔNG chứa logic chuyển trạng thái
- [ ] Protected no-arg constructor cho JPA
- [ ] Manual getters (không dùng Lombok)
- [ ] Package: `com.gpc.oms.domain`
