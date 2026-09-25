// Nguồn gốc AI: sinh từ docs/02-database-migration-spec.md, docs/01-domain-model.md
package com.gpc.oms.repository;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderRepository;
import com.gpc.oms.domain.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử tầng lưu trữ DataJpaTest cho {@link WorkOrderRepository} (Xác thực ràng buộc CSDL).
 */
@DataJpaTest
@DisplayName("Kiểm thử tầng dữ liệu WorkOrderRepository (Xác thực ràng buộc toàn vẹn)")
class WorkOrderRepositoryTest {

    @Autowired
    private WorkOrderRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Lưu mới một WorkOrder tự động sinh khóa chính UUID non-null")
    void persist_generatesUuidPrimaryKey() {
        WorkOrder wo = new WorkOrder("EQ-01", "Transformer issue", Priority.CRITICAL);
        assertThat(wo.getId()).isNull();

        WorkOrder saved = repository.saveAndFlush(wo);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEquipmentId()).isEqualTo("EQ-01");
        assertThat(saved.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByStatus lọc đúng các bản ghi theo trạng thái và hỗ trợ phân trang")
    void findByStatus_filtersAndPaginates() {
        WorkOrder wo1 = new WorkOrder("EQ-10", "Substation outage", Priority.HIGH);
        WorkOrder wo2 = new WorkOrder("EQ-11", "Fuse blown", Priority.MEDIUM);
        WorkOrder wo3 = new WorkOrder("EQ-12", "Meter replacement", Priority.LOW);

        repository.save(wo1);
        repository.save(wo2);

        wo3.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        repository.save(wo3);

        repository.flush();

        Page<WorkOrder> openOrders = repository.findByStatus(WorkOrderStatus.OPEN, PageRequest.of(0, 10));
        assertThat(openOrders.getTotalElements()).isEqualTo(2);
        assertThat(openOrders.getContent()).extracting(WorkOrder::getEquipmentId)
                .containsExactlyInAnyOrder("EQ-10", "EQ-11");

        Page<WorkOrder> inProgressOrders = repository.findByStatus(WorkOrderStatus.IN_PROGRESS, PageRequest.of(0, 10));
        assertThat(inProgressOrders.getTotalElements()).isEqualTo(1);
        assertThat(inProgressOrders.getContent().get(0).getEquipmentId()).isEqualTo("EQ-12");

        Page<WorkOrder> doneOrders = repository.findByStatus(WorkOrderStatus.DONE, PageRequest.of(0, 10));
        assertThat(doneOrders.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Ràng buộc kiểm tra chk_work_orders_priority từ chối chuỗi mức ưu tiên không hợp lệ")
    void checkConstraint_rejectsInvalidPriority() {
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("""
                INSERT INTO work_orders (id, equipment_id, description, priority, status, created_at)
                VALUES (RANDOM_UUID(), 'EQ-99', 'Test desc', 'INVALID_PRIORITY', 'OPEN', CURRENT_TIMESTAMP)
            """);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Ràng buộc kiểm tra chk_work_orders_status từ chối chuỗi trạng thái không hợp lệ")
    void checkConstraint_rejectsInvalidStatus() {
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("""
                INSERT INTO work_orders (id, equipment_id, description, priority, status, created_at)
                VALUES (RANDOM_UUID(), 'EQ-99', 'Test desc', 'HIGH', 'INVALID_STATUS', CURRENT_TIMESTAMP)
            """);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
