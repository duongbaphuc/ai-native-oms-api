<!--
Role: Principal SRE & Observability Architect
Task: Structured Logging Standard, Correlation ID Lifecycle, PII Masking, and Micrometer Metrics
Context files: docs/00-coding-rules.md, docs/00-security-rules.md, docs/00-api-rules.md
Constraints: Logback JSON / ECS format, SLF4J MDC, PII-free logs, Spring Boot Actuator & Prometheus
Target Files:
- src/main/java/com/gpc/oms/filter/CorrelationIdFilter.java
- src/main/resources/logback-spring.xml
- src/main/resources/application.yml (actuator & metrics config)
-->
# Observability, Structured Logging & Metrics Standard

Tài liệu này xác định các tiêu chuẩn giám sát vận hành (Observability), quy cách ghi nhật ký có cấu trúc (Structured Logging), chu trình sống của mã truy vết phân tán (Distributed Tracing), và các chỉ số đo lường hiệu năng (Metrics) cho microservice Outage Work Order.

Tài liệu là ngữ cảnh chuẩn mực để GitHub Copilot tự động sinh cấu hình Logback, Filter trích xuất `X-Correlation-Id`, các lệnh ghi log SLF4J không chứa PII, và các cấu hình Spring Boot Actuator/Micrometer.

---

## 1. Structured JSON Logging Architecture (ECS Format)

Mọi dòng log của ứng dụng khi xuất ra `stdout` trong môi trường Container bắt buộc phải ở định dạng **JSON một dòng (Single-line JSON)** tuân thủ theo chuẩn **Elastic Common Schema (ECS)**.

### Bảng Cấu trúc Trường Log Chuẩn (Log Event Schema)

| Tên Trường (JSON Key) | Kiểu Dữ liệu | Bắt buộc | Nguồn Dữ liệu | Ví dụ / Ý nghĩa |
|---|---|---|---|---|
| `@timestamp` | `String (ISO-8601)` | Có | Logback Engine | `"2026-09-23T09:40:00.123Z"` |
| `log.level` | `String` | Có | SLF4J Level | `"INFO"`, `"WARN"`, `"ERROR"`, `"DEBUG"` |
| `service.name` | `String` | Có | `application.yml` | `"ai-native-oms-api"` |
| `trace.id` | `String` | Có | SLF4J `MDC` | `"c9a1d48e-67bf-4e2a-9e12-b5e94b4cf9a1"` |
| `user.id` | `String` | Tùy chọn | Spring Security | `"dispatcher-01"` hoặc `"anonymous"` |
| `logger.name` | `String` | Có | Java Class Name | `"com.gpc.oms.service.WorkOrderService"` |
| `message` | `String` | Có | Developer input | Thông điệp sự kiện ngắn gọn, rõ ràng |
| `work_order_id` | `String (UUID)` | Tùy chọn | SLF4J `MDC` | `"550e8400-e29b-41d4-a716-446655440000"` |
| `action` | `String` | Tùy chọn | Business Logic | `"WORK_ORDER_CREATED"`, `"STATUS_ADVANCED"` |
| `error.type` | `String` | Khi có lỗi | Exception Class | `"IllegalStateException"` |
| `error.stack_trace` | `String` | Chỉ ở ERROR | Logback stacktrace | Chi tiết stack trace khi có lỗi nội bộ |

---

## 2. Distributed Tracing & Chu trình sống của Correlation ID

Để đảm bảo khả năng truy vết xuyên suốt các microservices trong hệ sinh thái OMS, mọi request phải được gắn một định danh truy vết duy nhất (`correlationId` / `traceId`).

### Luồng Hoạt động của `CorrelationIdFilter` (Step-by-Step Algorithm)

1. **Tiếp nhận Request:** Bộ lọc HTTP Filter chặn request đầu vào tại tầng Servlet.
2. **Kiểm tra Header:** Tìm kiếm header `X-Correlation-Id`.
   - Nếu tồn tại và hợp lệ: Sử dụng giá trị này làm `correlationId`.
   - Nếu không có: Sinh một UUID ngẫu nhiên mới: `UUID.randomUUID().toString()`.
3. **Nạp vào Ngữ cảnh MDC:** Gọi `MDC.put("traceId", correlationId)`.
4. **Phản hồi Header:** Ghi đè header vào response: `response.setHeader("X-Correlation-Id", correlationId)`.
5. **Tiếp tục Luồng:** Gọi `filterChain.doFilter(request, response)`.
6. **Dọn dẹp Bắt buộc (Cleanup):** Trong khối `finally`, luôn luôn thực thi `MDC.clear()` để tránh rò rỉ dữ liệu giữa các luồng tái sử dụng của Tomcat Thread Pool.

```java
// Logic minh họa cho CorrelationIdFilter
try {
    String correlationId = request.getHeader("X-Correlation-Id");
    if (correlationId == null || correlationId.isBlank()) {
        correlationId = UUID.randomUUID().toString();
    }
    MDC.put("traceId", correlationId);
    response.setHeader("X-Correlation-Id", correlationId);
    filterChain.doFilter(request, response);
} finally {
    MDC.clear(); // Bắt buộc để tránh ThreadLocal leak
}
```

---

## 3. Ma trận Cấp độ Log & Quy tắc Che Giấu PII (Data Masking)

### Ma trận Quyết định Cấp độ Log (Log Level Decision Tree)

| Level | Khi nào được phép dùng? | Ví dụ thực tế |
|---|---|---|
| **`ERROR`** | Lỗi bất ngờ làm gián đoạn luồng nghiệp vụ (Database down, 500 Unexpected, NullPointer). Cần kích hoạt cảnh báo On-call. | `log.error("Failed to persist WorkOrder due to DB timeout: {}", e.getMessage(), e);` |
| **`WARN`** | Vi phạm quy tắc nghiệp vụ nhưng hệ thống xử lý an toàn (Chuyển trạng thái sai, 404 Not Found, 429 Rate Limited). | `log.warn("Invalid state transition attempt from {} to {}", current, target);` |
| **`INFO`** | Mốc hoàn thành các tác vụ nghiệp vụ quan trọng (Tạo mới WorkOrder, phân công kỹ thuật viên, hoàn tất phiếu). | `log.info("WorkOrder created successfully with id: {}", workOrder.getId());` |
| **`DEBUG`** | Thông tin chi tiết phục vụ việc phân tích khi lập trình (Payload DTO, timing chi tiết từng method). | `log.debug("Evaluating transition condition for id: {}", id);` |

### Quy chuẩn Che giấu Dữ liệu Nhạy cảm (PII & Secret Masking)

> [!WARNING]
> Tuyệt đối không bao giờ log thông tin nhạy cảm: Mật khẩu, JWT Token, số điện thoại hoặc email của khách hàng báo mất điện.

### Bảng Quy tắc Regex Masking Tự động

| Loại Dữ liệu Nhạy cảm | Mẫu Regex Nhận diện | Quy tắc Thay thế (Masking) |
|---|---|---|
| **Bearer Token / Secret** | `(?i)bearer\s+[A-Za-z0-9\-._~+/]+=*` | `Bearer [MASKED_TOKEN]` |
| **Mật khẩu (Password)** | `(?i)"password"\s*:\s*"[^"]*"` | `"password": "******"` |
| **Số Điện thoại Khách hàng** | `(?:\+84\|0)[3\|5\|7\|8\|9]\d{8}` | `091****123` (Giữ 3 đầu, 3 cuối) |
| **Email Khách hàng** | `[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}` | `j***@domain.com` |

---

## 4. Chỉ số Đo lường & Giám sát (Actuator & Micrometer Metrics)

Ứng dụng tích hợp **Spring Boot Actuator** và xuất dữ liệu đo lường định dạng **Prometheus** để tích hợp vào hệ thống Grafana.

### Các Endpoints Giám sát Hệ thống

| Endpoint | Quyền hạn | Ý nghĩa |
|---|---|---|
| `/actuator/health` | Public | Liveness và Readiness probe phục vụ Kubernetes/Docker |
| `/actuator/info` | Public | Thông tin phiên bản build (`git.commit.id`, `version`) |
| `/actuator/prometheus` | `ROLE_ADMIN` | Dữ liệu metric định dạng Prometheus scraper |

### Bảng Chỉ số Đo lường Nghiệp vụ (Custom Business Metrics)

| Tên Metric (Prometheus Metric) | Kiểu (Type) | Nhãn (Tags) | Mục đích Đo lường |
|---|---|---|---|
| `oms_workorders_created_total` | Counter | `priority`, `status` | Tổng số phiếu sự cố được tạo mới |
| `oms_workorder_status_transitions_total`| Counter | `from_status`, `to_status` | Số lần chuyển đổi trạng thái thành công |
| `oms_workorder_resolution_time_seconds`| Timer | `priority` | Thời gian từ lúc tạo đến khi hoàn tất sự cố |
| `oms_workorder_active_count` | Gauge | `priority` | Số lượng phiếu sự cố đang mở (`OPEN`, `IN_PROGRESS`) |

---

## 5. Mẫu Ghi Log Chuẩn trong Tầng Service

```java
// Mẫu chuẩn khi thực thi nghiệp vụ trong WorkOrderService
public WorkOrderResponse advanceStatus(UUID id, WorkOrderStatus newStatus) {
    log.info("Request to advance WorkOrder status received [workOrderId={}, targetStatus={}]", id, newStatus);
    
    WorkOrder workOrder = repository.findById(id)
        .orElseThrow(() -> {
            log.warn("WorkOrder not found for status advancement [workOrderId={}]", id);
            return new ResourceNotFoundException("WorkOrder not found with id: " + id);
        });

    try {
        workOrder.advanceStatus(newStatus);
        WorkOrder saved = repository.save(workOrder);
        log.info("WorkOrder status advanced successfully [workOrderId={}, newStatus={}]", id, saved.getStatus());
        return WorkOrderResponse.from(saved);
    } catch (IllegalStateException e) {
        log.warn("Illegal state transition rejected [workOrderId={}, currentStatus={}, targetStatus={}]", 
            id, workOrder.getStatus(), newStatus);
        throw e;
    }
}
```
