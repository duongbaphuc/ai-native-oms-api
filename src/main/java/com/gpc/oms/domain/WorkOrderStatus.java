// AI Provenance: generated from docs/domain-model.md §Invariants, docs/api-spec.md §4
package com.gpc.oms.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum WorkOrderStatus {
    OPEN("Open"),
    IN_PROGRESS("InProgress"),
    DONE("Done");

    private final String value;

    WorkOrderStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Kiểm tra tính hợp lệ của chuyển trạng thái.
     * Bắt buộc: OPEN → IN_PROGRESS → DONE. Lùi hoặc nhảy cóc bị cấm tuyệt đối.
     */
    public boolean canTransitionTo(WorkOrderStatus next) {
        return switch (this) {
            case OPEN        -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == DONE;
            case DONE        -> false; // terminal — không có chuyển tiếp nào hợp lệ
        };
    }
}
