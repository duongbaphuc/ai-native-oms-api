// Nguồn gốc AI: sinh từ docs/01-domain-model.md §Invariants, docs/00-coding-rules.md
package com.gpc.oms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử đơn vị cho Gốc tập hợp (Aggregate Root) {@link WorkOrder}.
 */
@DisplayName("Kiểm thử đơn vị Aggregate Root WorkOrder")
class WorkOrderTest {

    @Test
    @DisplayName("Constructor mặc định không tham số tạo đối tượng non-null phục vụ JPA proxying")
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
    @DisplayName("Constructor có tham số khởi tạo đúng các bất biến mặc định: status=OPEN, resolvedAt=null, createdAt!=null")
    void constructor_setsDefaultValuesAndAllGetters() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        assertThat(wo.getId()).isNull(); // Được tự sinh khi lưu vào CSDL
        assertThat(wo.getEquipmentId()).isEqualTo("EQ-77");
        assertThat(wo.getDescription()).isEqualTo("Quá tải máy biến áp");
        assertThat(wo.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(wo.getCreatedAt()).isNotNull();
        assertThat(wo.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("advanceStatus() cho phép luồng chuyển trạng thái tuyến tính: OPEN -> IN_PROGRESS -> DONE và gán resolvedAt")
    void advanceStatus_allowsLinearFlow_andSetsResolvedAtOnDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        // Bước 1: Chuyển OPEN -> IN_PROGRESS
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(wo.getResolvedAt()).isNull(); // resolvedAt phải giữ giá trị null cho đến khi DONE

        // Bước 2: Chuyển IN_PROGRESS -> DONE
        wo.advanceStatus(WorkOrderStatus.DONE);
        assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.DONE);
        assertThat(wo.getResolvedAt()).isNotNull(); // Tự động ghi nhận mốc thời gian khi hoàn tất
    }

    @Test
    @DisplayName("advanceStatus() từ chối nhảy cóc trạng thái: OPEN -> DONE ném IllegalStateException")
    void advanceStatus_rejectsSkip_openToDone() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.DONE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from OPEN to DONE");
    }

    @Test
    @DisplayName("advanceStatus() từ chối tự chuyển đổi sang chính trạng thái hiện tại: OPEN -> OPEN")
    void advanceStatus_rejectsSelfTransition_openToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.OPEN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from OPEN to OPEN");
    }

    @Test
    @DisplayName("advanceStatus() từ chối quay lui trạng thái: IN_PROGRESS -> OPEN")
    void advanceStatus_rejectsRollback_inProgressToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.OPEN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from IN_PROGRESS to OPEN");
    }

    @Test
    @DisplayName("advanceStatus() từ chối quay lui trạng thái: DONE -> IN_PROGRESS")
    void advanceStatus_rejectsRollback_doneToInProgress() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.IN_PROGRESS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from DONE to IN_PROGRESS");
    }

    @Test
    @DisplayName("advanceStatus() từ chối quay lui trạng thái: DONE -> OPEN")
    void advanceStatus_rejectsRollback_doneToOpen() {
        WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);

        assertThatThrownBy(() -> wo.advanceStatus(WorkOrderStatus.OPEN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Invalid state transition from DONE to OPEN");
    }

    @Test
    @DisplayName("Constructor từ chối giá trị null của equipmentId với NullPointerException")
    void constructor_rejectsNullEquipmentId() {
        assertThatThrownBy(() -> new WorkOrder(null, "Description", Priority.HIGH))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("equipmentId must not be null");
    }

    @Test
    @DisplayName("Constructor từ chối giá trị null của description với NullPointerException")
    void constructor_rejectsNullDescription() {
        assertThatThrownBy(() -> new WorkOrder("EQ-01", null, Priority.HIGH))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("description must not be null");
    }

    @Test
    @DisplayName("Constructor từ chối giá trị null của priority với NullPointerException")
    void constructor_rejectsNullPriority() {
        assertThatThrownBy(() -> new WorkOrder("EQ-01", "Description", null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("priority must not be null");
    }

    @Test
    @DisplayName("advanceStatus từ chối giá trị null của newStatus với NullPointerException")
    void advanceStatus_rejectsNullNextStatus() {
        final WorkOrder wo = new WorkOrder("EQ-77", "Quá tải máy biến áp", Priority.HIGH);
        assertThatThrownBy(() -> wo.advanceStatus(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("newStatus must not be null");
    }
}
