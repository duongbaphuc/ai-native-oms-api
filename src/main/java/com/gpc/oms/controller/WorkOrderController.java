// AI Provenance: generated from docs/02-api-spec.md, docs/00-coding-rules.md, docs/drafts/draft-workorder-*.md
package com.gpc.oms.controller;

import com.gpc.oms.config.RoleConstants;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST Controller tiếp nhận các yêu cầu quản lý vòng đời Outage Work Order.
 *
 * <p>
 * Cung cấp các endpoints RESTful tiêu chuẩn cho phép tạo mới, tra cứu danh sách
 * có phân trang,
 * xem chi tiết và chuyển đổi trạng thái phiếu sự cố lưới điện.
 * </p>
 */
@RestController
@RequestMapping(WorkOrderController.PATH_WORKORDERS)
public class WorkOrderController {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderController.class);

    /** Đường dẫn cơ sở cho tài nguyên phiếu công tác WorkOrder. */
    public static final String PATH_WORKORDERS = "/api/v1/workorders";

    /** Đường dẫn chuyển trạng thái phiếu công tác. */
    public static final String PATH_STATUS = "/{id}/status";

    private final WorkOrderService workOrderService;

    public WorkOrderController(final WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    /**
     * Tiếp nhận và tạo mới một phiếu sự cố mất điện (Outage Work Order).
     *
     * @param request DTO chứa thông tin mã thiết bị, mô tả sự cố và độ ưu tiên
     * @return HTTP 201 Created cùng Header {@code Location} và thông tin phiếu sự
     *         cố vừa tạo
     */
    @PostMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_DISPATCHER_TECHNICIAN_OR_ADMIN)
    public ResponseEntity<WorkOrderResponse> createWorkOrder(@Valid @RequestBody final WorkOrderRequest request) {
        log.info("create workorder equipmentId={}", request.equipmentId().hashCode());
        final WorkOrderResponse response = workOrderService.createWorkOrder(request);
        final URI location = URI.create(PATH_WORKORDERS + "/" + response.id());

        return ResponseEntity.created(location).body(response);
    }

    /**
     * Tra cứu danh sách phiếu sự cố có phân trang và tùy chọn lọc theo trạng thái.
     *
     * @param pageable Tham số phân trang và sắp xếp (mặc định size=20,
     *                 sort=createdAt DESC)
     * @param status   Trạng thái phiếu sự cố cần lọc (OPEN, IN_PROGRESS, DONE)
     * @return HTTP 200 OK cùng {@link PagedResponse} danh sách phiếu sự cố
     */
    @GetMapping
    @PreAuthorize(RoleConstants.HAS_ROLE_DISPATCHER_TECHNICIAN_OR_ADMIN)
    public ResponseEntity<PagedResponse<WorkOrderResponse>> getWorkOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) final Pageable pageable,
            @RequestParam(required = false) final WorkOrderStatus status) {
        log.info("get workorders page={} size={} status={}", pageable.getPageNumber(), pageable.getPageSize(), status);
        final PagedResponse<WorkOrderResponse> response = workOrderService.getWorkOrders(pageable, status);

        return ResponseEntity.ok(response);
    }

    /**
     * Tra cứu thông tin chi tiết một phiếu sự cố theo định danh UUID duy nhất.
     *
     * @param id Khóa chính UUID của phiếu sự cố
     * @return HTTP 200 OK cùng chi tiết phiếu sự cố nếu tìm thấy
     */
    @GetMapping("/{id}")
    @PreAuthorize(RoleConstants.HAS_ROLE_DISPATCHER_TECHNICIAN_OR_ADMIN)
    public ResponseEntity<WorkOrderResponse> getWorkOrderById(@PathVariable final UUID id) {
        log.info("get workorder by id={}", id);
        final WorkOrderResponse response = workOrderService.getWorkOrderById(id);

        return ResponseEntity.ok(response);
    }

    /**
     * Cập nhật chuyển trạng thái vòng đời của phiếu sự cố theo máy trạng thái đơn
     * hướng.
     *
     * @param id      Khóa chính UUID của phiếu sự cố cần cập nhật
     * @param request DTO chứa trạng thái mới cần chuyển tiếp
     * @return HTTP 200 OK cùng thông tin phiếu sự cố sau khi cập nhật
     */
    @PatchMapping(PATH_STATUS)
    @PreAuthorize(RoleConstants.HAS_ROLE_TECHNICIAN_OR_ADMIN)
    public ResponseEntity<WorkOrderResponse> updateStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final WorkOrderStatusRequest request) {
        log.info("update status workorderId={}", id);
        final WorkOrderResponse response = workOrderService.updateStatus(id, request);

        return ResponseEntity.ok(response);
    }
}
