// AI Provenance: generated from docs/01-domain-model.md §Entities
package com.gpc.oms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Priority Enum Unit Tests")
class PriorityTest {

    @Test
    @DisplayName("values() contains all four priority levels in order")
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
    @DisplayName("valueOf() properly parses each enum name string")
    void valueOf_parsesStringCorrectly() {
        assertThat(Priority.valueOf("LOW")).isEqualTo(Priority.LOW);
        assertThat(Priority.valueOf("MEDIUM")).isEqualTo(Priority.MEDIUM);
        assertThat(Priority.valueOf("HIGH")).isEqualTo(Priority.HIGH);
        assertThat(Priority.valueOf("CRITICAL")).isEqualTo(Priority.CRITICAL);
    }
}
