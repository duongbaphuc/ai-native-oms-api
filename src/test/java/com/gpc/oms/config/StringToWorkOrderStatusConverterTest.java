// Nguồn gốc AI: sinh từ docs/02-api-spec.md §2, docs/01-domain-model.md §Invariants
package com.gpc.oms.config;

import com.gpc.oms.domain.WorkOrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị cho bộ chuyển đổi chuỗi sang enum {@link StringToWorkOrderStatusConverter}.
 */
@DisplayName("Kiểm thử đơn vị bộ chuyển đổi StringToWorkOrderStatusConverter")
class StringToWorkOrderStatusConverterTest {

    private final StringToWorkOrderStatusConverter converter = new StringToWorkOrderStatusConverter();

    @Test
    @DisplayName("Chuyển đổi thành công các chuỗi dạng hoa gạch dưới (UPPER_SNAKE) và dạng JsonValue")
    void convert_acceptsUpperSnakeAndJsonValueForms() {
        assertEquals(WorkOrderStatus.OPEN, converter.convert("OPEN"));
        assertEquals(WorkOrderStatus.OPEN, converter.convert("Open"));
        assertEquals(WorkOrderStatus.OPEN, converter.convert("open"));
        assertEquals(WorkOrderStatus.IN_PROGRESS, converter.convert("IN_PROGRESS"));
        assertEquals(WorkOrderStatus.IN_PROGRESS, converter.convert("InProgress"));
        assertEquals(WorkOrderStatus.DONE, converter.convert("DONE"));
        assertEquals(WorkOrderStatus.DONE, converter.convert("Done"));
    }

    @Test
    @DisplayName("Trả về null khi chuỗi đầu vào là null hoặc chỉ chứa khoảng trắng")
    void convert_returnsNullForNullOrBlank() {
        assertNull(converter.convert(null));
        assertNull(converter.convert(""));
        assertNull(converter.convert("   "));
    }

    @Test
    @DisplayName("Ném IllegalArgumentException khi chuỗi đầu vào không khớp với trạng thái nào")
    void convert_rejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("URGENT"));
    }
}
