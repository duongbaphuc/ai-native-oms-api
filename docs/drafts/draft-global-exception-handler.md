<!--
Role: Senior Engineer. Task: Tạo GlobalExceptionHandler trả lỗi theo chuẩn RFC 7807 Problem Details.
Context files: docs/api-rules.md, docs/security-rules.md
Constraints: 
- Map MethodArgumentNotValidException (400) -> invalidParams chi tiết.
- Map ResponseStatusException -> lấy status và message tương ứng.
- Tuyệt đối không trả về Stack Trace hoặc PII.
- Sử dụng ProblemDetail của Spring Boot 3.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Global Exception Handler (RFC 7807)

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Xử lý lỗi xác thực đầu vào (400 Bad Request)
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

    // Xử lý các lỗi nghiệp vụ (404 Not Found, 422 Unprocessable Entity, v.v.)
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatusCode(), ex.getReason());
        
        if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
            problem.setType(URI.create("https://api.oms.gpc.com/errors/not-found"));
        } else if (ex.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            problem.setType(URI.create("https://api.oms.gpc.com/errors/invalid-state-transition"));
        } else if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
            problem.setType(URI.create("https://api.oms.gpc.com/errors/forbidden"));
        }
        
        return problem;
    }
    / 403 — @PreAuthorize fail
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access Denied");
        problem.setType(URI.create("https://api.oms.gpc.com/errors/forbidden"));
        return problem;
    }

    // 400 — JSON malformed hoặc enum value không hợp lệ (vd. priority: "SUPER_HIGH")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex) {
        log.warn("Malformed request body");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed Request Body");
        problem.setType(URI.create("https://api.oms.gpc.com/errors/validation"));
        problem.setProperty("invalidParams",
            List.of(Map.of("name", "body", "reason", "Request body is malformed or contains an invalid enum value")));
        return problem;
    }

    // 500 — fallback cuối cùng, không lộ chi tiết nội bộ
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex); // full stacktrace CHỈ ở server log
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(URI.create("https://api.oms.gpc.com/errors/internal"));
        return problem;
    }
}
```
