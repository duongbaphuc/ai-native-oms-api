// AI Provenance: generated from docs/domain-model.md §Invariants
package com.gpc.oms.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorkOrderTest {

    @Test
    void advanceStatus_allowsLinearFlow_andSetsResolvedAtOnDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        assertEquals(WorkOrderStatus.OPEN, wo.getStatus());
        assertNull(wo.getResolvedAt());

        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        assertEquals(WorkOrderStatus.IN_PROGRESS, wo.getStatus());
        assertNull(wo.getResolvedAt());

        wo.advanceStatus(WorkOrderStatus.DONE);
        assertEquals(WorkOrderStatus.DONE, wo.getStatus());
        assertNotNull(wo.getResolvedAt());
    }

    @Test
    void advanceStatus_rejectsSkip_openToDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.DONE));
    }

    @Test
    void advanceStatus_rejectsRollback_doneToInProgress() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.IN_PROGRESS));
    }

    @Test
    void advanceStatus_rejectsRollback_inProgressToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        assertThrows(IllegalStateException.class, () -> wo.advanceStatus(WorkOrderStatus.OPEN));
    }

    @Test
    void constructor_setsDefaultValues() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải", Priority.HIGH);
        assertNotNull(wo.getCreatedAt());
        assertEquals(WorkOrderStatus.OPEN, wo.getStatus());
        assertNull(wo.getResolvedAt());
    }
}
