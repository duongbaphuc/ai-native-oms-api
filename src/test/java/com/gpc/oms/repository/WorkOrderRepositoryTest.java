// AI Provenance: generated from docs/database-migration-spec.md, docs/domain-model.md
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

@DataJpaTest
@DisplayName("WorkOrderRepository DataJpaTest (Persistence & Constraint Verification)")
class WorkOrderRepositoryTest {

    @Autowired
    private WorkOrderRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Persisting a new WorkOrder auto-generates a non-null UUID primary key")
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
    @DisplayName("findByStatus filters records matching given status and supports pagination")
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
        assertThat(openOrders.getContent()).extracting(WorkOrder::getEquipmentId).containsExactlyInAnyOrder("EQ-10", "EQ-11");

        Page<WorkOrder> inProgressOrders = repository.findByStatus(WorkOrderStatus.IN_PROGRESS, PageRequest.of(0, 10));
        assertThat(inProgressOrders.getTotalElements()).isEqualTo(1);
        assertThat(inProgressOrders.getContent().get(0).getEquipmentId()).isEqualTo("EQ-12");

        Page<WorkOrder> doneOrders = repository.findByStatus(WorkOrderStatus.DONE, PageRequest.of(0, 10));
        assertThat(doneOrders.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("Check constraint chk_work_orders_priority rejects invalid priority strings")
    void checkConstraint_rejectsInvalidPriority() {
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("""
                INSERT INTO work_orders (id, equipment_id, description, priority, status, created_at)
                VALUES (RANDOM_UUID(), 'EQ-99', 'Test desc', 'INVALID_PRIORITY', 'OPEN', CURRENT_TIMESTAMP)
            """);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Check constraint chk_work_orders_status rejects invalid status strings")
    void checkConstraint_rejectsInvalidStatus() {
        assertThatThrownBy(() -> {
            jdbcTemplate.execute("""
                INSERT INTO work_orders (id, equipment_id, description, priority, status, created_at)
                VALUES (RANDOM_UUID(), 'EQ-99', 'Test desc', 'HIGH', 'INVALID_STATUS', CURRENT_TIMESTAMP)
            """);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
