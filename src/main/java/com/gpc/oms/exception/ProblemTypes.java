// Nguồn gốc AI: sinh từ docs/02-api-spec.md §5, docs/00-api-rules.md §2
package com.gpc.oms.exception;

import java.net.URI;

/**
 * Tập trung các định danh URI cho định dạng chi tiết lỗi chuẩn hóa RFC 7807 Problem Details.
 *
 * <p>Các hằng số URI bất biến được cấp phát sẵn nhằm tối ưu hiệu năng bộ nhớ JVM
 * và loại bỏ các chuỗi ký tự ma thuật (magic strings) phân tán trong toàn hệ thống.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ProblemTypes {

    private ProblemTypes() {
        // Ngăn chặn khởi tạo thực thể lớp tiện ích (Tuân thủ Effective Java Item 4)
    }

    /** Lỗi dữ liệu yêu cầu không vượt qua kiểm tra ràng buộc xác thực (@Valid fail). */
    public static final URI VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");

    /** Lỗi thân yêu cầu JSON sai định dạng cú pháp hoặc giá trị enum không hợp lệ. */
    public static final URI MALFORMED_JSON = URI.create("urn:problem-type:malformed-json");

    /** Lỗi thiếu thông tin danh tính xác thực hoặc token hết hạn. */
    public static final URI UNAUTHORIZED = URI.create("urn:problem-type:unauthorized");

    /** Lỗi người dùng không có quyền truy cập tài nguyên (vi phạm RBAC). */
    public static final URI FORBIDDEN = URI.create("urn:problem-type:forbidden");

    /** Lỗi không tìm thấy tài nguyên theo khóa định danh yêu cầu. */
    public static final URI NOT_FOUND = URI.create("urn:problem-type:not-found");

    /** Lỗi vi phạm quy tắc chuyển dịch của máy trạng thái vòng đời phiếu công tác. */
    public static final URI INVALID_STATE_TRANSITION = URI.create("urn:problem-type:invalid-state-transition");

    /** Lỗi nội bộ không xác định phía máy chủ (không để lộ chi tiết nhạy cảm ra ngoài). */
    public static final URI INTERNAL_ERROR = URI.create("urn:problem-type:internal-error");

    /** Lỗi vượt quá giới hạn tần suất gọi API (HTTP 429 Too Many Requests). */
    public static final URI RATE_LIMIT_EXCEEDED = URI.create("urn:problem-type:rate-limit-exceeded");

    // Khóa thuộc tính mở rộng chuẩn RFC 7807 Problem Details
    public static final String PROPERTY_INVALID_PARAMS = "invalidParams";
    public static final String KEY_NAME = "name";
    public static final String KEY_REASON = "reason";
    public static final String FIELD_BODY = "body";

    // Tiêu đề lỗi chuẩn hóa
    public static final String TITLE_VALIDATION_FAILED = "Validation Failed";
    public static final String TITLE_MALFORMED_REQUEST_BODY = "Malformed Request Body";
    public static final String TITLE_ACCESS_DENIED = "Access Denied";
    public static final String TITLE_UNAUTHORIZED = "Unauthorized";
    public static final String TITLE_FORBIDDEN = "Forbidden";
    public static final String TITLE_TOO_MANY_REQUESTS = "Too Many Requests";

    // Thông điệp chi tiết chuẩn hóa
    public static final String DETAIL_MALFORMED_BODY =
            "Request body is malformed or contains an invalid enum value";
    public static final String DETAIL_UNAUTHORIZED_TOKEN =
            "Authentication token is missing or expired";
    public static final String DETAIL_FORBIDDEN_PERMISSION =
            "Access Denied: You do not have permission to access this resource";
    public static final String DETAIL_INTERNAL_ERROR =
            "An unexpected error occurred";
    public static final String REASON_INVALID_VALUE = "Invalid value";
}
