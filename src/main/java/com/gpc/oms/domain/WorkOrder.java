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
