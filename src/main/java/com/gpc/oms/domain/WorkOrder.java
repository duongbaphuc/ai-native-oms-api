// AI Provenance: generated from docs/domain-model.md, docs/coding-rules.md
package com.gpc.oms.domain;

import jakarta.persistence.*;
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
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant resolvedAt;

    protected WorkOrder() {} // JPA only — không gọi từ application code

    public WorkOrder(String equipmentId, String description, Priority priority) {
        this.equipmentId = equipmentId;
        this.description = description;
        this.priority = priority;
        this.status = WorkOrderStatus.OPEN;
        this.createdAt = Instant.now();
    }

    // --- Getters (manual, không dùng Lombok — theo coding-rules.md) ---
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
    public void advanceStatus(WorkOrderStatus newStatus) {
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
