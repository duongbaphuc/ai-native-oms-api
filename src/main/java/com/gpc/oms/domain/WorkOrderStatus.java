// Nguồn gốc AI: sinh từ docs/01-domain-model.md §Invariants, docs/02-api-spec.md §4
package com.gpc.oms.domain;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum biểu diễn các trạng thái trong vòng đời phiếu công tác xử lý sự cố mất điện.
 *
 * <p>Quy định máy trạng thái đơn hướng (one-way linear state machine):
 * {@code OPEN} &rarr; {@code IN_PROGRESS} &rarr; {@code DONE}. Mọi hành vi nhảy cóc hoặc quay lui
 * trạng thái đều bị cấm tuyệt đối theo ràng buộc bất biến miền nghiệp vụ.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public enum WorkOrderStatus {
    /**
     * Mở tiếp nhận — Phiếu công tác mới được tạo bởi điều độ viên (Dispatcher).
     */
    OPEN("Open"),

    /**
     * Đang thi công — Kỹ thuật viên (Technician) đang thực hiện xử lý ngoài hiện trường.
     */
    IN_PROGRESS("InProgress"),

    /**
     * Hoàn tất — Sự cố đã được xử lý khắc phục thành công và đóng phiếu.
     */
    DONE("Done");

    private final String value;

    WorkOrderStatus(String value) {
        this.value = value;
    }

    /**
     * Trả về chuỗi định dạng JSON chuẩn mực đại diện cho trạng thái.
     *
     * @return Chuỗi tên trạng thái (Open, InProgress, Done)
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Kiểm tra tính hợp lệ của việc chuyển đổi trạng thái tiếp theo.
     *
     * <p>Quy tắc bắt buộc: OPEN &rarr; IN_PROGRESS &rarr; DONE. Quay lui hoặc nhảy cóc bị từ chối.</p>
     *
     * @param next Trạng thái đích cần chuyển tới
     * @return {@code true} nếu chuyển đổi hợp lệ; {@code false} nếu không hợp lệ
     */
    public boolean canTransitionTo(WorkOrderStatus next) {
        return switch (this) {
            case OPEN        -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == DONE;
            case DONE        -> false; // Trạng thái kết thúc (terminal)
        };
    }
}
