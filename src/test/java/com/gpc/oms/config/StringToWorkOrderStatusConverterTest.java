// AI Provenance: generated from docs/02-api-spec.md §2, docs/01-domain-model.md §Invariants
package com.gpc.oms.config;

import com.gpc.oms.domain.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringToWorkOrderStatusConverterTest {

    private final StringToWorkOrderStatusConverter converter = new StringToWorkOrderStatusConverter();

    @Test
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
    void convert_returnsNullForNullOrBlank() {
        assertNull(converter.convert(null));
        assertNull(converter.convert(""));
        assertNull(converter.convert("   "));
    }

    @Test
    void convert_rejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class, () -> converter.convert("URGENT"));
    }
}
