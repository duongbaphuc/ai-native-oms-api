<!--
Role: Senior Engineer. Task: Tạo GlobalExceptionHandler trả lỗi theo chuẩn RFC 7807 Problem Details.
Context files: docs/api-rules.md, docs/security-rules.md
Constraints: 
- Map MethodArgumentNotValidException (400) -> invalidParams chi tiết.
- Map HttpMessageNotReadableException (400) -> malformed JSON / invalid enum.
- Map AccessDeniedException (403) -> @PreAuthorize fail.
- Map ResourceNotFoundException (404) -> ID không tồn tại trong CSDL.
- Map IllegalStateException (422) -> vi phạm quy tắc chuyển trạng thái state machine.
- Fallback Exception (500) -> không lộ stack trace / chi tiết nội bộ.
- Tuyệt đối không trả về Stack Trace hoặc PII.
- Sử dụng ProblemDetail của Spring Boot 3.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Global Exception Handler (RFC 7807)

## Target File

| File | Path | Action |
|---|---|---|
| `GlobalExceptionHandler` | `src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java` | NEW |

**Package:** `com.gpc.oms.exception`

## Error Mapping Summary Table

| # | Exception Class | HTTP | RFC 7807 `type` | Trigger | Ưu tiên |
|---|---|---|---|---|---|
| 1 | `MethodArgumentNotValidException` | 400 | `urn:problem-type:validation-error` | `@Valid` fail (`@NotBlank`, `@NotNull`, `@Size`) | Cao nhất (specific) |
| 2 | `HttpMessageNotReadableException` | 400 | `urn:problem-type:malformed-json` | JSON malformed, enum value không hợp lệ, `ignoreUnknown=false` reject | Cao |
| 3 | `AccessDeniedException` | 403 | `urn:problem-type:forbidden` | `@PreAuthorize` fail (role không đủ) | Cao |
| 4 | `ResourceNotFoundException` | 404 | `urn:problem-type:not-found` | Service throw khi `findById` trả empty | Cao |
| 5 | `IllegalStateException` | 422 | `urn:problem-type:invalid-state-transition` | Entity throw từ `advanceStatus()` khi vi phạm state machine | Cao |
| 6 | `Exception` | 500 | `urn:problem-type:internal-error` | Fallback — bắt mọi exception không xử lý | Thấp nhất (catch-all) |

> [!IMPORTANT]
> **Thứ tự ưu tiên handler:** Spring `@ExceptionHandler` chọn handler cụ thể nhất trước (most specific first). `AccessDeniedException` sẽ luôn được bắt bởi handler #3, `ResourceNotFoundException` bởi handler #4, `IllegalStateException` bởi handler #5, và handler #6 (`Exception.class`) chỉ đóng vai trò fallback cuối cùng cho các lỗi không lường trước.

## Handler Code

```java
// AI Provenance: generated from docs/api-rules.md §2, docs/security-rules.md §4
package com.gpc.oms.exception;

import com.gpc.oms.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
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
        problem.setType(URI.create("urn:problem-type:validation-error"));
        
        final List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> Map.of(
                "name", error.getField(),
                "reason", error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
            )).toList();
            
        problem.setProperty("invalidParams", invalidParams);
        return problem;
    }

    // Handler #2: 400 — JSON malformed hoặc enum value không hợp lệ
    // Trigger: priority: "URGENT", body không parse được, ignoreUnknown=false reject
    // Response: invalidParams[].name="body"
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(final HttpMessageNotReadableException ex) {
        log.warn("Malformed request body");
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed Request Body");
        problem.setType(URI.create("urn:problem-type:malformed-json"));
        problem.setProperty("invalidParams",
            List.of(Map.of("name", "body", "reason", "Request body is malformed or contains an invalid enum value")));
        return problem;
    }

    // Handler #3: 400 — Query/path param conversion fail (vd: ?status=URGENT)
    // Trigger: StringToWorkOrderStatusConverter quăng IllegalArgumentException,
    // Spring MVC wrap thành MethodArgumentTypeMismatchException
    // Response: invalidParams[].name = tên param (vd: "status")
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleQueryParamTypeMismatch(final org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.warn("Query parameter type mismatch: {}", ex.getName());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Failed");
        problem.setType(URI.create("urn:problem-type:validation-error"));
        problem.setProperty("invalidParams",
            List.of(Map.of("name", ex.getName(), "reason", "Invalid value for parameter '" + ex.getName() + "'")));
        return problem;
    }

    // Handler #4: 403 — @PreAuthorize fail
    // Trigger: AccessDeniedException từ Spring Security khi role không đủ
    // Import: org.springframework.security.access.AccessDeniedException (KHÔNG phải java.nio.file.AccessDeniedException)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(final AccessDeniedException ex) {
        log.warn("Access denied");
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access Denied");
        problem.setType(URI.create("urn:problem-type:forbidden"));
        return problem;
    }

    // Handler #5: 404 — Resource không tìm thấy
    // Trigger: Service throw ResourceNotFoundException khi findById trả empty
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(final ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setType(URI.create("urn:problem-type:not-found"));
        return problem;
    }

    // Handler #6: 422 — Vi phạm state machine (invalid state transition)
    // Trigger: Entity throw IllegalStateException qua advanceStatus(), Service re-throw
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateTransition(final IllegalStateException ex) {
        log.warn("Illegal state transition: {}", ex.getMessage());
        final ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setType(URI.create("urn:problem-type:invalid-state-transition"));
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
        problem.setType(URI.create("urn:problem-type:internal-error"));
        return problem;
    }
}
```

## Checklist

- [ ] `@RestControllerAdvice` trên class
- [ ] Import `org.springframework.security.access.AccessDeniedException` (KHÔNG `java.nio.file.AccessDeniedException`)
- [ ] 7 handlers đầy đủ: Validation(400), MalformedJSON(400), TypeMismatch(400), AccessDenied(403), ResourceNotFound(404), IllegalState(422), Fallback(500)
- [ ] `ProblemDetail` (Spring Boot 3) — KHÔNG dùng custom error class
- [ ] Không trả stack trace, SQL message, class name ra client
- [ ] Log: `log.warn` cho 4xx, `log.error` cho 5xx
- [ ] RFC 7807 `type` URI khớp `api-spec.md §5`
- [ ] Package: `com.gpc.oms.exception`
- [ ] Oracle Senior Java Style: Sử dụng `final` cho parameters và local variables để tối ưu JIT Escape Analysis.
