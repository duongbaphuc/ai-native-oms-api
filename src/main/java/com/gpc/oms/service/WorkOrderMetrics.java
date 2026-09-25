// Nguồn gốc AI: sinh từ docs/00-coding-rules.md, docs/02-observability-and-logging.md
package com.gpc.oms.service;

/**
 * Tập trung các hằng số định danh Micrometer metrics và nhãn phân loại (tags) cho hệ thống OMS.
 *
 * <p>Loại bỏ hoàn toàn các chuỗi ký tự ma thuật (magic strings) phân tán trong việc đo lường,
 * giám sát và thu thập chỉ số vận hành hệ thống.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public final class WorkOrderMetrics {

    private WorkOrderMetrics() {
        // Ngăn chặn khởi tạo thực thể lớp tiện ích (Effective Java Item 4)
    }

    /** Tên metric bộ đếm tổng số phiếu sự cố được tạo mới. */
    public static final String COUNTER_CREATED = "oms_workorders_created_total";

    /** Tên metric bộ đếm tổng số lần chuyển đổi trạng thái phiếu sự cố. */
    public static final String COUNTER_TRANSITIONS = "oms_workorder_status_transitions_total";

    /** Khóa tag mức độ ưu tiên của phiếu sự cố. */
    public static final String TAG_PRIORITY = "priority";

    /** Khóa tag trạng thái hiện tại của phiếu sự cố. */
    public static final String TAG_STATUS = "status";

    /** Khóa tag trạng thái nguồn trước khi chuyển đổi. */
    public static final String TAG_FROM_STATUS = "from_status";

    /** Khóa tag trạng thái đích sau khi chuyển đổi. */
    public static final String TAG_TO_STATUS = "to_status";
}
