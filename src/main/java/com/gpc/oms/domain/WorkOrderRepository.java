// AI Provenance: generated from docs/domain-model.md, docs/database-migration-spec.md §3
package com.gpc.oms.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    Page<WorkOrder> findByStatus(WorkOrderStatus status, Pageable pageable);
}
