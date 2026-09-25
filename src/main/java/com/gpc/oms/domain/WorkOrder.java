// Nguồn gốc AI: sinh từ docs/01-domain-model.md, docs/00-coding-rules.md
package com.gpc.oms.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Gốc tập hợp (Aggregate Root) và Thực thể JPA đại diện cho Phiếu công tác xử lý sự cố mất điện.
 *
 * <p>Quản lý toàn bộ thông tin mã thiết bị, mô tả sự cố, độ ưu tiên, trạng thái vòng đời
 * và mốc thời gian giải quyết sự cố theo các ràng buộc bất biến nghiệp vụ.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@Entity
@Table(name = "work_orders")
public class WorkOrder {

    /** Độ dài tối đa cho mã định danh thiết bị lưới điện. */
    public static final int MAX_EQUIPMENT_ID_LENGTH = 50;

    /** Độ dài tối thiểu cho mô tả sự cố. */
    public static final int MIN_DESCRIPTION_LENGTH = 10;

    /** Độ dài tối đa cho mô tả sự cố. */
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    /** Độ dài tối đa cho chuỗi lưu trữ Enum (Priority, WorkOrderStatus). */
    public static final int MAX_ENUM_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = MAX_EQUIPMENT_ID_LENGTH)
    private String equipmentId;

    @Column(nullable = false, length = MAX_DESCRIPTION_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = MAX_ENUM_LENGTH)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = MAX_ENUM_LENGTH)
    private WorkOrderStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = true)
    private Instant resolvedAt;

    /**
     * Constructor mặc định phục vụ JPA proxying — Không sử dụng trực tiếp trong mã ứng dụng.
     */
    protected WorkOrder() {}

    /**
     * Khởi tạo một phiếu công tác mới với trạng thái mặc định là {@link WorkOrderStatus#OPEN}.
     *
     * @param equipmentId Mã định danh thiết bị điện gặp sự cố (không được null)
     * @param description Mô tả chi tiết hiện trường sự cố (không được null)
     * @param priority Mức độ ưu tiên xử lý (không được null)
     */
    public WorkOrder(final String equipmentId, final String description, final Priority priority) {
        this.equipmentId = Objects.requireNonNull(equipmentId, "equipmentId must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.status = WorkOrderStatus.OPEN;
        this.createdAt = Instant.now();
    }

    // --- Các phương thức Getter thủ công (Tuân thủ docs/00-coding-rules.md, không dùng Lombok) ---

    public UUID getId() { return id; }
    public String getEquipmentId() { return equipmentId; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public WorkOrderStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    /**
     * Chuyển trạng thái phiếu công tác theo quy tắc máy trạng thái đơn hướng bất biến.
     *
     * <p>Ủy quyền kiểm tra tính hợp lệ sang {@link WorkOrderStatus#canTransitionTo(WorkOrderStatus)}.
     * Khi chuyển sang trạng thái {@link WorkOrderStatus#DONE}, tự động cập nhật mốc thời gian {@code resolvedAt}.</p>
     *
     * @param newStatus Trạng thái mới cần chuyển tiếp tới
     * @throws IllegalStateException nếu hành vi chuyển đổi trạng thái vi phạm quy tắc máy trạng thái
     */
    public void advanceStatus(final WorkOrderStatus newStatus) {
        Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Invalid state transition from " + this.status + " to " + newStatus);
        }
        this.status = newStatus;
        if (this.status == WorkOrderStatus.DONE) {
            this.resolvedAt = Instant.now();
        }
    }
}
