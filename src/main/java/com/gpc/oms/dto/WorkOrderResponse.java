// Nguồn gốc AI: sinh từ docs/02-api-spec.md §1–§4, docs/01-domain-model.md
package com.gpc.oms.dto;

import com.gpc.oms.domain.Priority;
import com.gpc.oms.domain.WorkOrder;
import com.gpc.oms.domain.WorkOrderStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Record DTO phản hồi dữ liệu chi tiết của phiếu công tác xử lý sự cố mất điện.
 *
 * <p>Bao gồm định danh UUID, mã thiết bị, mô tả sự cố, độ ưu tiên, trạng thái,
 * mốc thời gian tiếp nhận và mốc thời gian xử lý hoàn tất.</p>
 *
 * @param id Định danh duy nhất (UUID) của phiếu công tác
 * @param equipmentId Mã định danh thiết bị lưới điện gặp sự cố
 * @param description Mô tả chi tiết hiện trường sự cố
 * @param priority Mức độ ưu tiên xử lý
 * @param status Trạng thái hiện tại trong vòng đời phiếu (Open, InProgress, Done)
 * @param createdAt Mốc thời gian tiếp nhận tạo phiếu (chuẩn UTC)
 * @param resolvedAt Mốc thời gian hoàn tất xử lý (chuẩn UTC, null khi chưa hoàn tất)
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public record WorkOrderResponse(
    UUID             id,
    String           equipmentId,
    String           description,
    Priority         priority,
    WorkOrderStatus  status,
    Instant          createdAt,
    Instant          resolvedAt   // null cho đến khi status = DONE
) {

    /**
     * Phương thức nhà máy (Factory method): chuyển đổi từ Thực thể JPA sang DTO phản hồi.
     *
     * <p>Được triệu gọi bởi tầng {@code WorkOrderService} sau mỗi thao tác tạo hoặc cập nhật.</p>
     *
     * @param entity Thực thể {@link WorkOrder} trong cơ sở dữ liệu
     * @return Đối tượng {@link WorkOrderResponse} tương ứng
     */
    public static WorkOrderResponse from(final WorkOrder entity) {
        Objects.requireNonNull(entity, "workOrder must not be null");
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
