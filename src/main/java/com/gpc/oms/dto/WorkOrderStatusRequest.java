// Nguồn gốc AI: sinh từ docs/02-api-spec.md §4, docs/01-domain-model.md §Invariants
package com.gpc.oms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gpc.oms.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Record DTO chứa payload yêu cầu chuyển trạng thái vòng đời của phiếu công tác (PATCH /status).
 *
 * <p>Áp dụng kiểm tra ràng buộc không được null và từ chối các trường lạ.</p>
 *
 * @param status Trạng thái mới cần chuyển tiếp tới (hợp lệ: Open, InProgress, Done)
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record WorkOrderStatusRequest(

    @NotNull(message = "status must not be null; valid values: Open, InProgress, Done")
    WorkOrderStatus status

) {}
