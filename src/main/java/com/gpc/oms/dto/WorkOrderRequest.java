// Nguồn gốc AI: sinh từ docs/02-api-spec.md §1, docs/01-domain-model.md, docs/00-coding-rules.md
package com.gpc.oms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gpc.oms.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Record DTO chứa payload yêu cầu tiếp nhận và tạo mới một phiếu sự cố mất điện (Outage Work Order).
 *
 * <p>Áp dụng kiểm tra tính hợp lệ dữ liệu đầu vào nghiêm ngặt qua Jakarta Bean Validation.
 * Cấu hình {@code ignoreUnknown = false} nhằm từ chối các trường lạ không được định nghĩa trước.</p>
 *
 * @param equipmentId Mã định danh thiết bị lưới điện gặp sự cố (bắt buộc, tối đa 50 ký tự)
 * @param description Mô tả chi tiết hiện trường sự cố (bắt buộc, từ 10 đến 500 ký tự)
 * @param priority Mức độ ưu tiên xử lý sự cố (bắt buộc: LOW, MEDIUM, HIGH, CRITICAL)
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public record WorkOrderRequest(

    @NotBlank(message = "equipmentId must not be blank")
    @Size(max = 50, message = "equipmentId must not exceed 50 characters")
    String equipmentId,

    @NotBlank(message = "description must not be blank")
    @Size(min = 10, max = 500, message = "description must be between 10 and 500 characters")
    String description,

    @NotNull(message = "priority must not be null; valid values: LOW, MEDIUM, HIGH, CRITICAL")
    Priority priority

) {}
