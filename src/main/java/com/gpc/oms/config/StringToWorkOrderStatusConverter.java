// AI Provenance: generated from docs/api-spec.md §2, docs/domain-model.md §Invariants
package com.gpc.oms.config;

import com.gpc.oms.domain.WorkOrderStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToWorkOrderStatusConverter implements Converter<String, WorkOrderStatus> {

    @Override
    public WorkOrderStatus convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        for (WorkOrderStatus status : WorkOrderStatus.values()) {
            if (status.name().equalsIgnoreCase(source) || status.getValue().equalsIgnoreCase(source)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown WorkOrderStatus: " + source);
    }
}
