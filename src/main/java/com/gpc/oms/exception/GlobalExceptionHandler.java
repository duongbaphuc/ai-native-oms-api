// Nguồn gốc AI: sinh từ docs/00-api-rules.md §2, docs/00-security-rules.md §4,
// docs/drafts/draft-global-exception-handler.md
package com.gpc.oms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ toàn cục tập trung (Centralized Global Exception Handler) cho toàn bộ ứng dụng OMS API.
 *
 * <p>Đón bắt các ngoại lệ runtime, chuyển đổi thành cấu trúc phản hồi lỗi chuẩn hóa quốc tế
 * theo đặc tả RFC 7807 Problem Details (Content-Type: {@code application/problem+json}).
 * Bảo đảm che giấu hoàn toàn các thông tin kỹ thuật nhạy cảm (stack trace, SQL) đối với client.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Xử lý lỗi #1: HTTP 400 — Lỗi xác thực dữ liệu đầu vào (@Valid fail)
    // Tác nhân kích hoạt: Vi phạm ràng buộc @NotBlank, @NotNull, @Size
    // Dữ liệu phản hồi: invalidParams[] chứa tên trường dữ liệu và lý do vi phạm
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(final MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ProblemTypes.TITLE_VALIDATION_FAILED);
        problem.setType(ProblemTypes.VALIDATION_ERROR);
        
        final List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        final List<Map<String, String>> invalidParams = new ArrayList<>(fieldErrors.size());
        for (final FieldError error : fieldErrors) {
            final String reason = error.getDefaultMessage() != null
                    ? error.getDefaultMessage()
                    : ProblemTypes.REASON_INVALID_VALUE;
            invalidParams.add(Map.of(
                    ProblemTypes.KEY_NAME, error.getField(),
                    ProblemTypes.KEY_REASON, reason));
        }
            
        problem.setProperty(ProblemTypes.PROPERTY_INVALID_PARAMS, invalidParams);
        return problem;
    }

    // Xử lý lỗi #2: HTTP 400 — Thân yêu cầu JSON sai định dạng cú pháp hoặc enum không hợp lệ
    // Tác nhân: priority: "URGENT", JSON không parse được, hoặc ignoreUnknown=false từ chối trường lạ
    // Dữ liệu phản hồi: invalidParams[].name="body"
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(final HttpMessageNotReadableException ex) {
        log.warn("Malformed request body");
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ProblemTypes.TITLE_MALFORMED_REQUEST_BODY);
        problem.setType(ProblemTypes.MALFORMED_JSON);
        problem.setProperty(ProblemTypes.PROPERTY_INVALID_PARAMS,
            List.of(Map.of(
                    ProblemTypes.KEY_NAME, ProblemTypes.FIELD_BODY,
                    ProblemTypes.KEY_REASON, ProblemTypes.DETAIL_MALFORMED_BODY)));
        return problem;
    }

    // Xử lý lỗi #3: HTTP 400 — Lỗi ép kiểu tham số truy vấn/đường dẫn (ví dụ: ?status=URGENT)
    // Tác nhân kích hoạt: StringToWorkOrderStatusConverter ném IllegalArgumentException,
    // được Spring MVC bao đóng thành MethodArgumentTypeMismatchException
    // Dữ liệu phản hồi: invalidParams[].name = tên tham số (ví dụ: "status")
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleQueryParamTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        log.warn("Query parameter type mismatch: {}", ex.getName());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ProblemTypes.TITLE_VALIDATION_FAILED);
        problem.setType(ProblemTypes.VALIDATION_ERROR);
        problem.setProperty(ProblemTypes.PROPERTY_INVALID_PARAMS,
            List.of(Map.of(
                    ProblemTypes.KEY_NAME, ex.getName(),
                    ProblemTypes.KEY_REASON, "Invalid value for parameter '" + ex.getName() + "'")));
        return problem;
    }

    // Xử lý lỗi #4: HTTP 403 — Từ chối truy cập do không đủ phân quyền (@PreAuthorize fail)
    // Tác nhân: AccessDeniedException từ Spring Security khi vai trò của người dùng không thỏa mãn
    // Thư viện: org.springframework.security.access.AccessDeniedException (không phải của java.nio.file)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException ex) {
        log.warn("Access denied");
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, ProblemTypes.TITLE_ACCESS_DENIED);
        problem.setType(ProblemTypes.FORBIDDEN);
        return problem;
    }

    // Xử lý lỗi #5: HTTP 404 — Tài nguyên yêu cầu không tồn tại
    // Tác nhân kích hoạt: Tầng Service ném ResourceNotFoundException khi findById trả về Optional.empty()
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(ProblemTypes.NOT_FOUND);
        return problem;
    }

    // Xử lý lỗi #6: HTTP 422 — Vi phạm quy tắc máy trạng thái (chuyển trạng thái không hợp lệ)
    // Tác nhân kích hoạt: Thực thể Domain ném IllegalStateException qua advanceStatus()
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateTransition(final IllegalStateException ex) {
        log.warn("Illegal state transition: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(ProblemTypes.INVALID_STATE_TRANSITION);
        return problem;
    }

    // Xử lý lỗi #7: HTTP 500 — Chốt chặn ngoại lệ cuối cùng, ngăn chặn rò rỉ dữ liệu nhạy cảm
    // Tác nhân: Bất kỳ ngoại lệ không mong muốn nào chưa được xử lý ở các hàm trên
    // Dữ liệu phản hồi: Thông điệp chung, tuyệt đối không lộ stack trace, SQL hay tên lớp nội bộ
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception ex) {
        log.error("Unexpected error", ex); // Chi tiết lỗi chỉ được ghi lại tại server log
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, ProblemTypes.DETAIL_INTERNAL_ERROR);
        problem.setType(ProblemTypes.INTERNAL_ERROR);
        return problem;
    }
}
