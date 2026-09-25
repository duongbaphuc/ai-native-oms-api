// Nguồn gốc AI: sinh từ docs/01-domain-model.md §Invariants, docs/02-api-spec.md §4
package com.gpc.oms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho Enum {@link WorkOrderStatus} và máy trạng thái đơn hướng.
 */
@DisplayName("Kiểm thử đơn vị Enum WorkOrderStatus và máy trạng thái")
class WorkOrderStatusTest {

    @Test
    @DisplayName("getValue() trả về chuỗi biểu diễn JSON theo định dạng JsonValue")
    void getValue_returnsCorrectString() {
        assertThat(WorkOrderStatus.OPEN.getValue()).isEqualTo("Open");
        assertThat(WorkOrderStatus.IN_PROGRESS.getValue()).isEqualTo("InProgress");
        assertThat(WorkOrderStatus.DONE.getValue()).isEqualTo("Done");
    }

    @Test
    @DisplayName("values() và valueOf() chứa đầy đủ 3 hằng số trạng thái")
    void enumValuesAndValueOf() {
        WorkOrderStatus[] values = WorkOrderStatus.values();
        assertThat(values).containsExactly(
            WorkOrderStatus.OPEN,
            WorkOrderStatus.IN_PROGRESS,
            WorkOrderStatus.DONE
        );
        assertThat(WorkOrderStatus.valueOf("OPEN")).isEqualTo(WorkOrderStatus.OPEN);
        assertThat(WorkOrderStatus.valueOf("IN_PROGRESS")).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(WorkOrderStatus.valueOf("DONE")).isEqualTo(WorkOrderStatus.DONE);
    }

    @ParameterizedTest(name = "canTransitionTo: {0} -> {1} mong đợi {2}")
    @CsvSource({
        "OPEN,        OPEN,        false",
        "OPEN,        IN_PROGRESS, true",
        "OPEN,        DONE,        false",
        "IN_PROGRESS, OPEN,        false",
        "IN_PROGRESS, IN_PROGRESS, false",
        "IN_PROGRESS, DONE,        true",
        "DONE,        OPEN,        false",
        "DONE,        IN_PROGRESS, false",
        "DONE,        DONE,        false"
    })
    @DisplayName("Kiểm thử toàn bộ 9 hoán vị chuyển trạng thái cho canTransitionTo")
    void canTransitionTo_allPermutations(WorkOrderStatus current, WorkOrderStatus next, boolean expected) {
        assertThat(current.canTransitionTo(next)).isEqualTo(expected);
    }
}
