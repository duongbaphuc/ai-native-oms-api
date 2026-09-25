// Nguồn gốc AI: sinh từ docs/02-api-spec.md §2, docs/01-domain-model.md §Invariants
package com.gpc.oms.config;

import com.gpc.oms.domain.WorkOrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Bộ chuyển đổi Spring Converter hỗ trợ phân tích chuỗi ký tự thành enum {@link WorkOrderStatus}.
 *
 * <p>Cho phép tiếp nhận cả hai định dạng chuỗi: dạng chữ hoa gạch dưới (OPEN, IN_PROGRESS, DONE)
 * hoặc dạng chữ hoa camel chuẩn JSON (Open, InProgress, Done) mà không phân biệt chữ hoa chữ thường.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@Component
public class StringToWorkOrderStatusConverter implements Converter<String, WorkOrderStatus> {

    private static final WorkOrderStatus[] VALUES = WorkOrderStatus.values();

    /**
     * Chuyển đổi chuỗi đầu vào thành hằng số {@link WorkOrderStatus} tương ứng.
     *
     * @param source Chuỗi ký tự cần chuyển đổi
     * @return Hằng số {@link WorkOrderStatus} tương ứng, hoặc {@code null} nếu chuỗi rỗng
     * @throws IllegalArgumentException nếu chuỗi không khớp với bất kỳ trạng thái nào
     */
    @Override
    public WorkOrderStatus convert(final String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        for (final WorkOrderStatus status : VALUES) {
            if (status.name().equalsIgnoreCase(source) || status.getValue().equalsIgnoreCase(source)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown WorkOrderStatus: " + source);
    }
}
