// AI Provenance: generated from docs/00-api-rules.md §2, docs/00-security-rules.md §4,
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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handler #1: 400 — Xử lý lỗi xác thực đầu vào (@Valid fail)
    // Trigger: @NotBlank, @NotNull, @Size violations
    // Response: invalidParams[] chứa field name + reason
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(final MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Failed");
        problem.setType(ProblemTypes.VALIDATION_ERROR);
        
        final List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        final List<Map<String, String>> invalidParams = new ArrayList<>(fieldErrors.size());
        for (final FieldError error : fieldErrors) {
            final String reason = error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value";
            invalidParams.add(Map.of("name", error.getField(), "reason", reason));
        }
            
        problem.setProperty("invalidParams", invalidParams);
        return problem;
    }

    // Handler #2: 400 — JSON malformed hoặc enum value không hợp lệ
    // Trigger: priority: "URGENT", body không parse được, ignoreUnknown=false reject
    // Response: invalidParams[].name="body"
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(final HttpMessageNotReadableException ex) {
        log.warn("Malformed request body");
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Malformed Request Body");
        problem.setType(ProblemTypes.MALFORMED_JSON);
        problem.setProperty("invalidParams",
            List.of(Map.of("name", "body", "reason", "Request body is malformed or contains an invalid enum value")));
        return problem;
    }

    // Handler #3: 400 — Query/path param conversion fail (vd: ?status=URGENT)
    // Trigger: StringToWorkOrderStatusConverter quăng IllegalArgumentException,
    // Spring MVC wrap thành MethodArgumentTypeMismatchException
    // Response: invalidParams[].name = tên param (vd: "status")
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleQueryParamTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        log.warn("Query parameter type mismatch: {}", ex.getName());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Failed");
        problem.setType(ProblemTypes.VALIDATION_ERROR);
        problem.setProperty("invalidParams",
            List.of(Map.of("name", ex.getName(), "reason", "Invalid value for parameter '" + ex.getName() + "'")));
        return problem;
    }

    // Handler #4: 403 — @PreAuthorize fail
    // Trigger: AccessDeniedException từ Spring Security khi role không đủ
    // Import: org.springframework.security.access.AccessDeniedException
    // (KHÔNG phải java.nio.file.AccessDeniedException)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException ex) {
        log.warn("Access denied");
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access Denied");
        problem.setType(ProblemTypes.FORBIDDEN);
        return problem;
    }

    // Handler #5: 404 — Resource không tìm thấy
    // Trigger: Service throw ResourceNotFoundException khi findById trả empty
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(ProblemTypes.NOT_FOUND);
        return problem;
    }

    // Handler #6: 422 — Vi phạm state machine (invalid state transition)
    // Trigger: Entity throw IllegalStateException qua advanceStatus(), Service re-throw
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateTransition(final IllegalStateException ex) {
        log.warn("Illegal state transition: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(ProblemTypes.INVALID_STATE_TRANSITION);
        return problem;
    }

    // Handler #7: 500 — Fallback cuối cùng, không lộ chi tiết nội bộ
    // Trigger: mọi Exception không khớp handler #1-#6
    // Response: message chung, KHÔNG lộ stack trace / SQL / class name
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(final Exception ex) {
        log.error("Unexpected error", ex); // full stacktrace CHỈ ở server log, KHÔNG trả về client
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(ProblemTypes.INTERNAL_ERROR);
        return problem;
    }
}
