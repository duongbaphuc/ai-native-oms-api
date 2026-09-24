// AI Provenance: generated from docs/api-spec.md, docs/coding-rules.md, docs/drafts/draft-workorder-*.md
package com.gpc.oms.controller;

import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.PagedResponse;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;
import com.gpc.oms.service.WorkOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workorders")
public class WorkOrderController {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderController.class);
    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<WorkOrderResponse> createWorkOrder(@Valid @RequestBody final WorkOrderRequest request) {
        log.info("create workorder equipmentId={}", request.equipmentId().hashCode());
        final WorkOrderResponse response = workOrderService.createWorkOrder(request);
        final URI location = URI.create("/api/v1/workorders/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<PagedResponse<WorkOrderResponse>> getWorkOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable,
            @RequestParam(required = false) final WorkOrderStatus status) {
        log.info("get workorders page={} size={} status={}", pageable.getPageNumber(), pageable.getPageSize(), status);
        final PagedResponse<WorkOrderResponse> response = workOrderService.getWorkOrders(pageable, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<WorkOrderResponse> getWorkOrderById(@PathVariable final UUID id) {
        log.info("get workorder by id={}", id);
        final WorkOrderResponse response = workOrderService.getWorkOrderById(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<WorkOrderResponse> updateStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final WorkOrderStatusRequest request) {
        log.info("update status workorderId={}", id);
        final WorkOrderResponse response = workOrderService.updateStatus(id, request);
        return ResponseEntity.ok(response);
    }
}
