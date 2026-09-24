// AI Provenance: generated from docs/01-domain-model.md §Invariants, docs/02-api-spec.md §4
package com.gpc.oms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkOrderStatus Enum Unit Tests")
class WorkOrderStatusTest {

    @Test
    @DisplayName("getValue() returns expected JSON string representation")
    void getValue_returnsCorrectString() {
        assertThat(WorkOrderStatus.OPEN.getValue()).isEqualTo("Open");
        assertThat(WorkOrderStatus.IN_PROGRESS.getValue()).isEqualTo("InProgress");
        assertThat(WorkOrderStatus.DONE.getValue()).isEqualTo("Done");
    }

    @Test
    @DisplayName("values() and valueOf() contain all 3 status constants")
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

    @ParameterizedTest(name = "canTransitionTo: {0} -> {1} expected {2}")
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
    @DisplayName("Test all 9 state transition permutations for canTransitionTo")
    void canTransitionTo_allPermutations(WorkOrderStatus current, WorkOrderStatus next, boolean expected) {
        assertThat(current.canTransitionTo(next)).isEqualTo(expected);
    }
}
