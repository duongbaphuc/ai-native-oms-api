// Nguồn gốc AI: sinh từ docs/01-domain-model.md §Entities
package com.gpc.oms.domain;

/**
 * Enum biểu diễn mức độ ưu tiên xử lý sự cố mất điện (Outage Work Order Priority).
 *
 * <p>Được sử dụng để phân loại mức độ khẩn cấp nhằm điều phối nhân lực kỹ thuật.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public enum Priority {
    /**
     * Mức độ ưu tiên thấp — Sự cố nhỏ, không ảnh hưởng diện rộng.
     */
    LOW,

    /**
     * Mức độ ưu tiên trung bình — Cần kiểm tra định kỳ hoặc xử lý theo ca làm việc.
     */
    MEDIUM,

    /**
     * Mức độ ưu tiên cao — Nguy cơ mất điện diện rộng, cần xử lý trong thời gian ngắn.
     */
    HIGH,

    /**
     * Mức độ ưu tiên khẩn cấp — Trạm biến áp hoặc đường dây truyền tải huyết mạch gặp sự cố.
     */
    CRITICAL
}
