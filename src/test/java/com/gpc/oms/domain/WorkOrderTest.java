// AI Provenance: generated from docs/domain-model.md §Invariants, docs/coding-rules.md
package com.gpc.oms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WorkOrder Aggregate Root Unit Tests")
class WorkOrderTest {

    @Test
    @DisplayName("Default no-arg constructor creates non-null entity for JPA proxying")
    void noArgConstructor_forJpa() {
        WorkOrder wo = new WorkOrder();
        assertThat(wo).isNotNull();
        assertThat(wo.getId()).isNull();
        assertThat(wo.getEquipmentId()).isNull();
        assertThat(wo.getDescription()).isNull();
        assertThat(wo.getPriority()).isNull();
        assertThat(wo.getStatus()).isNull();
        assertThat(wo.getCreatedAt()).isNull();
        assertThat(wo.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("Parametric constructor initializes default invariants: status=OPEN, resolvedAt=null, createdAt!=null")
    void constructor_setsDefaultValuesAndAllGetters() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        assertThat(wo.getId()).isNull(); // generated upon DB persist
        assertThat(wo.getEquipmentId()).isEqualTo("EQ-77");
        assertThat(wo.getDescription()).isEqualTo("Quá tải máy biến áp");
        assertThat(wo.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(wo.getCreatedAt()).isNotNull();
        assertThat(wo.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("advanceStatus() permits linear progression: OPEN -> IN_PROGRESS -> DONE and stamps resolvedAt")
    void advanceStatus_allowsLinearFlow_andSetsResolvedAtOnDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        // Step 1: Advance OPEN -> IN_PROGRESS
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(wo.getResolvedAt()).isNull(); // ResolvedAt must remain null until DONE

        // Step 2: Advance IN_PROGRESS -> DONE
        wo.advanceStatus(WorkOrderStatus.DONE);
        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.DONE);
        assertThat(wo.getResolvedAt()).isNotNull(); // Automatically timestamped upon DONE
    }

    @Test
    @DisplayName("advanceStatus() rejects skip progression: OPEN -> DONE with IllegalStateException")
    void advanceStatus_rejectsSkip_openToDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.DONE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from OPEN to DONE");
    }

    @Test
    @DisplayName("advanceStatus() rejects self transition: OPEN -> OPEN")
    void advanceStatus_rejectsSelfTransition_openToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.OPEN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from OPEN to OPEN");
    }

    @Test
    @DisplayName("advanceStatus() rejects rollback: IN_PROGRESS -> OPEN")
    void advanceStatus_rejectsRollback_inProgressToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.OPEN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from IN_PROGRESS to OPEN");
    }

    @Test
    @DisplayName("advanceStatus() rejects rollback: DONE -> IN_PROGRESS")
    void advanceStatus_rejectsRollback_doneToInProgress() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.IN_PROGRESS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from DONE to IN_PROGRESS");
    }

    @Test
    @DisplayName("advanceStatus() rejects rollback: DONE -> OPEN")
    void advanceStatus_rejectsRollback_doneToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.OPEN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from DONE to OPEN");
    }

    @Test
    @DisplayName("constructor rejects null equipmentId with NullPointerException")
    void constructor_rejectsNullEquipmentId() {
        assertThatThrownBy(() -> new WorkOrder(null, "Description", Priority.HIGH))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("equipmentId must not be null");
    }

    @Test
    @DisplayName("constructor rejects null description with NullPointerException")
    void constructor_rejectsNullDescription() {
        assertThatThrownBy(() -> new WorkOrder("EQ-01", null, Priority.HIGH))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("description must not be null");
    }

    @Test
    @DisplayName("constructor rejects null priority with NullPointerException")
    void constructor_rejectsNullPriority() {
        assertThatThrownBy(() -> new WorkOrder("EQ-01", "Description", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("priority must not be null");
    }

    @Test
    @DisplayName("advanceStatus rejects null next status with NullPointerException")
    void advanceStatus_rejectsNullNextStatus() {
        final WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        assertThatThrownBy(() -> wo.advanceStatus(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("newStatus must not be null");
    }
}
