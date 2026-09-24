// AI Provenance: generated from docs/api-spec.md §4, docs/domain-model.md §Invariants
package com.gpc.oms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gpc.oms.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public record WorkOrderStatusRequest(

    @NotNull(message = "status must not be null; valid values: Open, InProgress, Done")
    WorkOrderStatus status

) {}
