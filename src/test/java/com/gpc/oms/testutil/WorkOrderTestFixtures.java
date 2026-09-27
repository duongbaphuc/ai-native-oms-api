// Nguồn gốc AI: sinh từ docs/00-internal-coding-standards.md
package com.gpc.oms.testutil;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderStatus;
import com.gpc.oms.dto.WorkOrderRequest;
import com.gpc.oms.dto.WorkOrderResponse;
import com.gpc.oms.dto.WorkOrderStatusRequest;

import java.time.Instant;
import java.util.UUID;

/**
 * Tiện ích tạo dữ liệu mẫu kiểm thử theo mẫu thiết kế Object Mother (Test Data Fixtures).
 *
 * <p>Tập trung hóa các hàm sinh đối tượng Domain Entity và DTOs dùng chung cho các lớp kiểm thử,
 * loại bỏ mã nguồn trùng lặp trong toàn bộ test suite.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public final class WorkOrderTestFixtures {

    private WorkOrderTestFixtures() {
        // Ngăn chặn khởi tạo thực thể lớp tiện ích (Effective Java Item 4)
    }

    public static final String DEFAULT_EQUIPMENT_ID = "EQ-100";
    public static final String DEFAULT_DESCRIPTION = "Faulty transformer inspection required";
    public static final Priority DEFAULT_PRIORITY = Priority.HIGH;

    /**
     * Khởi tạo đối tượng thực thể {@link WorkOrder} với các giá trị mặc định.
     */
    public static WorkOrder createDefaultEntity() {
        return new WorkOrder(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
    }

    /**
     * Khởi tạo đối tượng thực thể {@link WorkOrder} với tham số tùy chọn.
     */
    public static WorkOrder createEntity(final String equipmentId, final String description, final Priority priority) {
        return new WorkOrder(equipmentId, description, priority);
    }

    /**
     * Khởi tạo đối tượng thực thể {@link WorkOrder} ở trạng thái hoàn tất (DONE).
     */
    public static WorkOrder createDoneEntity(final String equipmentId,
                                            final String description,
                                            final Priority priority) {
        final WorkOrder wo = new WorkOrder(equipmentId, description, priority);
        wo.advanceStatus(WorkOrderStatus.IN_PROGRESS);
        wo.advanceStatus(WorkOrderStatus.DONE);

        return wo;
    }

    /**
     * Khởi tạo đối tượng DTO {@link WorkOrderRequest} với các giá trị mặc định.
     */
    public static WorkOrderRequest createDefaultRequest() {
        return new WorkOrderRequest(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
    }

    /**
     * Khởi tạo đối tượng DTO {@link WorkOrderRequest} với tham số tùy chọn.
     */
    public static WorkOrderRequest createRequest(final String equipmentId,
                                                final String description,
                                                final Priority priority) {
        return new WorkOrderRequest(equipmentId, description, priority);
    }

    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    /**
     * Khởi tạo đối tượng DTO {@link WorkOrderStatusRequest} với trạng thái mong muốn.
     */
    public static WorkOrderStatusRequest createStatusRequest(final WorkOrderStatus status) {
        return new WorkOrderStatusRequest(status);
    }

    /**
     * Khởi tạo đối tượng DTO {@link WorkOrderResponse} với đầy đủ các trường dữ liệu.
     */
    public static WorkOrderResponse createResponse(final UUID id, final String equipmentId, final String description,
                                                  final Priority priority, final WorkOrderStatus status,
                                                  final Instant createdAt, final Instant resolvedAt) {
        return new WorkOrderResponse(id, equipmentId, description, priority, status, createdAt, resolvedAt);
    }

    /**
     * Khởi tạo đối tượng DTO {@link WorkOrderResponse} mặc định với ID chỉ định.
     */
    public static WorkOrderResponse createDefaultResponse(final UUID id) {
        return createResponse(id, DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY,
                WorkOrderStatus.OPEN, Instant.now(), null);
    }

    /**
     * Khởi tạo đối tượng DTO {@link WorkOrderResponse} với trạng thái chỉ định.
     */
    public static WorkOrderResponse createResponse(final UUID id, final WorkOrderStatus status) {
        final Instant now = Instant.now();

        return createResponse(id, DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY,
                status, now, status == WorkOrderStatus.DONE ? now : null);
    }

    /**
     * Tạo chuỗi JSON mẫu cho payload tạo mới WorkOrderRequest.
     */
    public static String createRequestJson(final String equipmentId, final String description, final String priority) {
        return """
            {"equipmentId":"%s","description":"%s","priority":"%s"}"""
            .formatted(equipmentId, description, priority);
    }

    /**
     * Tạo chuỗi JSON mặc định cho payload tạo mới WorkOrderRequest.
     */
    public static String createDefaultRequestJson() {
        return createRequestJson(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY.name());
    }

    /**
     * Tạo chuỗi JSON mẫu cho payload cập nhật trạng thái WorkOrderStatusRequest.
     */
    public static String createStatusRequestJson(final String status) {
        return """
            {"status":"%s"}"""
            .formatted(status);
    }
}
