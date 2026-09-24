// AI Provenance: test fixture for reusable object creation across test classes
package com.gpc.oms.testutil;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard Object Mother & Test Data Fixture for OMS tests.
 * Centralizes sample entity and DTO creation to eliminate code duplication across test classes.
 */
public final class WorkOrderTestFixtures {

    private WorkOrderTestFixtures() {
        // Enforce non-instantiability
    }

    public static final String DEFAULT_EQUIPMENT_ID = "EQ-100";
    public static final String DEFAULT_DESCRIPTION = "Faulty transformer inspection required";
    public static final Priority DEFAULT_PRIORITY = Priority.HIGH;

    public static WorkOrder createDefaultEntity() {
        return new WorkOrder(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
    }

    public static WorkOrder createEntity(final String equipmentId, final String description, final Priority priority) {
        return new WorkOrder(equipmentId, description, priority);
    }

    public static WorkOrder createDoneEntity(final String equipmentId, final String description, final Priority priority) {
        final WorkOrder wo = new WorkOrder(equipmentId, description, priority);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);
        return wo;
    }

    public static WorkOrderRequest createDefaultRequest() {
        return new WorkOrderRequest(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
    }

    public static WorkOrderRequest createRequest(final String equipmentId, final String description, final Priority priority) {
        return new WorkOrderRequest(equipmentId, description, priority);
    }

    public static WorkOrderStatusRequest createStatusRequest(final WorkOrderStatus status) {
        return new WorkOrderStatusRequest(status);
    }

    public static WorkOrderResponse createResponse(final UUID id, final String equipmentId, final String description,
                                                  final Priority priority, final WorkOrderStatus status,
                                                  final Instant createdAt, final Instant resolvedAt) {
        return new WorkOrderResponse(id, equipmentId, description, priority, status, createdAt, resolvedAt);
    }
}
