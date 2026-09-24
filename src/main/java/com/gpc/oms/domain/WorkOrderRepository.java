// AI Provenance: generated from docs/domain-model.md
package com.gpc.oms.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {}
