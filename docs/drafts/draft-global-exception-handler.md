<!--
Role: Senior Engineer. Task: Tạo GlobalExceptionHandler trả lỗi theo chuẩn RFC 7807 Problem Details.
Context files: docs/api-rules.md, docs/security-rules.md
Constraints: 
- Map MethodArgumentNotValidException (400) -> invalidParams chi tiết.
- Map ResponseStatusException -> lấy status và message tương ứng.
- Map AccessDeniedException (403) -> @PreAuthorize fail.
- Map HttpMessageNotReadableException (400) -> malformed JSON / invalid enum.
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
| 1 | `MethodArgumentNotValidException` | 400 | `.../errors/validation` | `@Valid` fail (`@NotBlank`, `@NotNull`, `@Size`) | Cao nhất (specific) |
| 2 | `HttpMessageNotReadableException` | 400 | `.../errors/validation` | JSON malformed, enum value không hợp lệ, `ignoreUnknown=false` reject | Cao |
| 3 | `AccessDeniedException` | 403 | `.../errors/forbidden` | `@PreAuthorize` fail (role không đủ) | Cao |
| 4 | `ResponseStatusException` | Varies | Varies (404/422/...) | Service throw cho NOT_FOUND, UNPROCESSABLE_ENTITY | Trung bình |
| 5 | `Exception` | 500 | `.../errors/internal` | Fallback — bắt mọi exception không xử lý | Thấp nhất (catch-all) |

> [!IMPORTANT]
> **Thứ tự ưu tiên handler:** Spring `@ExceptionHandler` chọn handler cụ thể nhất trước (most specific first). `AccessDeniedException` sẽ luôn được bắt bởi handler #3, KHÔNG bởi `ResponseStatusException` handler #4, vì `AccessDeniedException` không phải subclass của `ResponseStatusException`.

## Handler Code

```java
// AI Provenance: generated from docs/api-rules.md §2, docs/security-rules.md §4
package com.gpc.oms.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

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
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation Failed");
        problem.setType(URI.create("https://api.oms.gpc.com/errors/validation"));
        
        List<Map<String, String>> invalidParams = ex.getBindingResult().getFieldErrors().stream()
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
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed Request Body");
        problem.setType(URI.create("https://api.oms.gpc.com/errors/validation"));
        problem.setProperty("invalidParams",
            List.of(Map.of("name", "body", "reason", "Request body is malformed or contains an invalid enum value")));
        return problem;
    }

    // Handler #3: 403 — @PreAuthorize fail
    // Trigger: AccessDeniedException từ Spring Security khi role không đủ
    // Import: org.springframework.security.access.AccessDeniedException (KHÔNG phải java.nio.file.AccessDeniedException)
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access Denied");
        problem.setType(URI.create("https://api.oms.gpc.com/errors/forbidden"));
        return problem;
    }

    // Handler #4: 404/422/etc — Xử lý các lỗi nghiệp vụ
    // Trigger: Service throw ResponseStatusException(NOT_FOUND) hoặc ResponseStatusException(UNPROCESSABLE_ENTITY)
    // Response: type URI based on status code
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            problem.setType(URI.create("https://api.oms.gpc.com/errors/not-found"));
        } else if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            problem.setType(URI.create("https://api.oms.gpc.com/errors/invalid-state-transition"));
        }
        
        return problem;
    }

    // Handler #5: 500 — Fallback cuối cùng, không lộ chi tiết nội bộ
    // Trigger: mọi Exception không khớp handler #1-#4
    // Response: message chung, KHÔNG lộ stack trace / SQL / class name
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex); // full stacktrace CHỈ ở server log, KHÔNG trả về client
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://api.oms.gpc.com/errors/internal"));
        return problem;
    }
}
```

## Checklist

- [ ] `@RestControllerAdvice` trên class
- [ ] Import `org.springframework.security.access.AccessDeniedException` (KHÔNG `java.nio.file.AccessDeniedException`)
- [ ] 5 handlers đầy đủ: Validation(400), MalformedJSON(400), AccessDenied(403), ResponseStatus(4xx), Fallback(500)
- [ ] `ProblemDetail` (Spring Boot 3) — KHÔNG dùng custom error class
- [ ] Không trả stack trace, SQL message, class name ra client
- [ ] Log: `log.warn` cho 4xx, `log.error` cho 5xx
- [ ] RFC 7807 `type` URI khớp `api-spec.md §5`
- [ ] Package: `com.gpc.oms.exception`
