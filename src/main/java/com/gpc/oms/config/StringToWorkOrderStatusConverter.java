// AI Provenance: generated from docs/api-spec.md §2, docs/domain-model.md §Invariants
package com.gpc.oms.config;

import com.gpc.oms.domain.WorkOrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToWorkOrderStatusConverter implements Converter<String, WorkOrderStatus> {

    private static final WorkOrderStatus[] VALUES = WorkOrderStatus.values();

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
