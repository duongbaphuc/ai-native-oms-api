// AI Provenance: generated from docs/api-spec.md §1–§4, docs/domain-model.md
package com.gpc.oms.dto;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderResponse(
    UUID id,
    String equipmentId,
    String description,
    Priority priority,
    WorkOrderStatus status,
    Instant createdAt,
    Instant resolvedAt // null cho đến khi status = DONE
) {

    /**
     * Factory method: convert JPA Entity → Response DTO.
     * Được gọi bởi WorkOrderService sau mỗi thao tác CRUD.
     */
    public static WorkOrderResponse from(WorkOrder entity) {
        return new WorkOrderResponse(
            entity.getId(),
            entity.getEquipmentId(),
            entity.getDescription(),
            entity.getPriority(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getResolvedAt()
        );
    }
}
