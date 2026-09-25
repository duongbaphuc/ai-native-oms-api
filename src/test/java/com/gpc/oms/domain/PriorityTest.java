// Nguồn gốc AI: sinh từ docs/01-domain-model.md §Entities
package com.gpc.oms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho Enum {@link Priority}.
 */
@DisplayName("Kiểm thử đơn vị Enum Priority")
class PriorityTest {

    @Test
    @DisplayName("values() chứa đầy đủ 4 cấp độ ưu tiên theo đúng thứ tự khai báo")
    void values_containsAllEnums() {
        Priority[] values = Priority.values();
        assertThat(values).containsExactly(
            Priority.LOW,
            Priority.MEDIUM,
            Priority.HIGH,
            Priority.CRITICAL
        );
    }

    @Test
    @DisplayName("valueOf() phân tích chính xác chuỗi ký tự thành enum tương ứng")
    void valueOf_parsesStringCorrectly() {
        assertThat(Priority.valueOf("LOW")).isEqualTo(Priority.LOW);
        assertThat(Priority.valueOf("MEDIUM")).isEqualTo(Priority.MEDIUM);
        assertThat(Priority.valueOf("HIGH")).isEqualTo(Priority.HIGH);
        assertThat(Priority.valueOf("CRITICAL")).isEqualTo(Priority.CRITICAL);
    }
}
