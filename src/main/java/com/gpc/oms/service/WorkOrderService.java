// AI Provenance: generated from docs/00-coding-rules.md, docs/00-api-rules.md, docs/01-domain-model.md, docs/00-internal-coding-standards.md
package com.gpc.oms.service;

import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderRepository;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.PagedResponse;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import com.gpc.oms.exception.ResourceNotFoundException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class WorkOrderService {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);
    private final WorkOrderRepository repo;
    private final MeterRegistry registry;

    public WorkOrderService(WorkOrderRepository repo, MeterRegistry registry) {
        this.repo = repo;
        this.registry = registry;
    }

    public WorkOrderResponse createWorkOrder(final WorkOrderRequest req) {
        final WorkOrder entity = new WorkOrder(req.equipmentId(), req.description(), req.priority());
        final WorkOrder saved = repo.save(entity);
        registry.counter("oms_workorders_created_total",
            "priority", saved.getPriority().name(), "status", saved.getStatus().name()).increment();
        log.info("created workorder id={}", saved.getId());
        return WorkOrderResponse.from(saved);
    }

    public PagedResponse<WorkOrderResponse> getWorkOrders(final Pageable pageable, final WorkOrderStatus status) {
        final Page<WorkOrder> page = (status != null)
                ? repo.findByStatus(status, pageable)
                : repo.findAll(pageable);
        return PagedResponse.from(page.map(WorkOrderResponse::from));
    }

    public WorkOrderResponse getWorkOrderById(final UUID id) {
        final WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));
        return WorkOrderResponse.from(entity);
    }

    public WorkOrderResponse updateStatus(final UUID id, final WorkOrderStatusRequest req) {
        final WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));

        final WorkOrderStatus fromStatus = entity.getStatus();
        try {
            entity.advanceStatus(req.status());
        } catch (IllegalStateException ex) {
            throw ex; // Re-throw — GlobalExceptionHandler sẽ map thành 422
        }

        final WorkOrder saved = repo.save(entity);
        registry.counter("oms_workorder_status_transitions_total",
            "from_status", fromStatus.name(), "to_status", saved.getStatus().name()).increment();
        log.info("updated workorder id={} status={}", saved.getId(), saved.getStatus());
        return WorkOrderResponse.from(saved);
    }
}
