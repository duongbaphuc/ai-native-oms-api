// Nguồn gốc AI: sinh từ docs/02-api-spec.md §3, docs/00-api-rules.md §3
package com.gpc.oms.exception;

/**
 * Ngoại lệ runtime ném ra khi không tìm thấy tài nguyên theo định danh được yêu cầu trong cơ sở dữ liệu.
 *
 * <p>Được ánh xạ thành HTTP 404 Not Found kèm RFC 7807 Problem Details
 * bởi {@link GlobalExceptionHandler}.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Khởi tạo ngoại lệ với thông điệp mô tả chi tiết tài nguyên không tìm thấy.
     *
     * @param message Thông điệp giải thích lý do không tìm thấy tài nguyên
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
