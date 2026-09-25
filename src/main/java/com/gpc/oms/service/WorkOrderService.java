// AI Provenance: generated from docs/00-coding-rules.md, docs/00-api-rules.md,
// docs/01-domain-model.md, docs/00-internal-coding-standards.md
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

/**
 * Service điều phối nghiệp vụ cho vòng đời Outage Work Order.
 *
 * <p>Đảm nhiệm chuyển đổi giữa DTOs và JPA Entity, tương tác lưu trữ
 * qua {@link WorkOrderRepository}, ủy quyền kiểm tra bất biến máy trạng thái
 * cho Domain Entity, và ghi nhận số liệu Micrometer.</p>
 */
@Service
public class WorkOrderService {
    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);
    private final WorkOrderRepository repo;
    private final MeterRegistry registry;

    public WorkOrderService(WorkOrderRepository repo, MeterRegistry registry) {
        this.repo = repo;
        this.registry = registry;
    }

    /**
     * Tạo mới một phiếu sự cố mất điện (Outage Work Order) với trạng thái mặc định OPEN.
     *
     * @param req DTO chứa thông tin mã thiết bị, mô tả và độ ưu tiên
     * @return {@link WorkOrderResponse} đại diện cho phiếu sự cố vừa được tạo
     */
    public WorkOrderResponse createWorkOrder(final WorkOrderRequest req) {
        final WorkOrder entity = new WorkOrder(req.equipmentId(), req.description(), req.priority());
        final WorkOrder saved = repo.save(entity);
        registry.counter("oms_workorders_created_total",
            "priority", saved.getPriority().name(), "status", saved.getStatus().name()).increment();
        log.info("created workorder id={}", saved.getId());
        return WorkOrderResponse.from(saved);
    }

    /**
     * Tra cứu danh sách phiếu sự cố có phân trang và tùy chọn lọc theo trạng thái.
     *
     * @param pageable Thông tin phân trang và sắp xếp
     * @param status Trạng thái cần lọc (có thể null nếu muốn lấy tất cả)
     * @return {@link PagedResponse} chứa danh sách phiếu sự cố và metadata phân trang
     */
    public PagedResponse<WorkOrderResponse> getWorkOrders(final Pageable pageable, final WorkOrderStatus status) {
        final Page<WorkOrder> page = (status != null)
                ? repo.findByStatus(status, pageable)
                : repo.findAll(pageable);
        return PagedResponse.from(page.map(WorkOrderResponse::from));
    }

    /**
     * Tra cứu thông tin chi tiết của một phiếu sự cố theo định danh UUID duy nhất.
     *
     * @param id Khóa chính UUID của phiếu sự cố
     * @return {@link WorkOrderResponse} chi tiết của phiếu sự cố
     * @throws ResourceNotFoundException nếu không tìm thấy phiếu sự cố với ID đã cho
     */
    public WorkOrderResponse getWorkOrderById(final UUID id) {
        final WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));
        return WorkOrderResponse.from(entity);
    }

    /**
     * Cập nhật chuyển trạng thái vòng đời của phiếu sự cố (OPEN -> IN_PROGRESS -> DONE).
     *
     * @param id Khóa chính UUID của phiếu sự cố cần cập nhật
     * @param req DTO chứa trạng thái mới
     * @return {@link WorkOrderResponse} sau khi đã cập nhật trạng thái thành công
     * @throws ResourceNotFoundException nếu không tìm thấy phiếu sự cố với ID đã cho
     * @throws IllegalStateException nếu vi phạm quy tắc chuyển trạng thái của máy trạng thái
     */
    public WorkOrderResponse updateStatus(final UUID id, final WorkOrderStatusRequest req) {
        final WorkOrder entity = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder not found with id: " + id));

        final WorkOrderStatus fromStatus = entity.getStatus();
        entity.advanceStatus(req.status());

        final WorkOrder saved = repo.save(entity);
        registry.counter("oms_workorder_status_transitions_total",
            "from_status", fromStatus.name(), "to_status", saved.getStatus().name()).increment();
        log.info("updated workorder id={} status={}", saved.getId(), saved.getStatus());
        return WorkOrderResponse.from(saved);
    }
}
