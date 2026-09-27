<!--
Role: Principal Software Architect & Standards Lead
Task: Define internal coding conventions, date/time timezone policy, object mapping rules, pagination standards, and resilience patterns
Context files: docs/00-coding-rules.md, docs/00-api-rules.md, docs/01-domain-model.md, docs/02-api-spec.md
Constraints: Java 17+, Spring Boot 3.3, UTC Instant, MapStruct / Static Factory (NO Reflection ModelMapper), RFC 7807 consistency
-->
# Internal Coding Standards & Technical Utilities Guide

Tài liệu này xác định các quy chuẩn kỹ thuật vi mô (Micro-conventions) và các thư viện tiện ích dùng chung trong toàn bộ hệ thống Outage Work Order API.

Tài liệu cung cấp ngữ cảnh chi tiết để GitHub Copilot tự động sinh code xử lý thời gian, chuyển đổi DTO/Entity, cấu trúc phân trang, kiểm tra tính hợp lệ dữ liệu (Custom Validation), và cơ chế chịu lỗi (Resilience / Retry) mà không làm phát sinh xung đột phong cách mã nguồn hoặc suy giảm hiệu năng.

---

## 1. Date, Time & Timezone Policy (Chính sách Xử lý Thời gian)

Để tránh hoàn toàn các lỗi lệch múi giờ trong hệ thống điều hành mất điện (đặc biệt khi giao tiếp giữa các trạm biến áp ở các múi giờ khác nhau):

1. **Lưu trữ & Tính toán:** Toàn bộ tầng Domain, Service và Database bắt buộc sử dụng **UTC** (`java.time.Instant`).
2. **Giao tiếp REST API:** Sử dụng chuỗi định dạng chuẩn **ISO-8601 Extended** kết hợp độ lệch múi giờ (UTC Offset): `yyyy-MM-dd'T'HH:mm:ssXXX` (vd: `2026-09-23T07:30:00Z`).
3. **Cấm Tuyệt Đối:** Không sử dụng các kiểu dữ liệu thời gian cũ của Java: `java.util.Date`, `java.sql.Date`, `java.sql.Timestamp`, `java.util.Calendar`.

### Bảng Quy chuẩn Xử lý Kiểu Thời gian

| Tầng Ứng dụng | Kiểu Dữ liệu Bắt buộc | Định dạng Lưu trữ / Xuất | Quy tắc & Annotation |
|---|---|---|---|
| **Database** | `TIMESTAMPTZ` (Postgres) / `TIMESTAMP WITH TIME ZONE` (H2) | Chuẩn thời gian UTC | Ánh xạ qua Hibernate tự động |
| **Domain Entity** | `java.time.Instant` | Unix Epoch Timestamp / UTC | `@Column(name = "created_at", nullable = false)` |
| **DTO Response** | `java.time.Instant` | ISO-8601 UTC String | Jackson serialize mặc định dạng ISO-8601 |
| **DTO Request** | `java.time.Instant` | ISO-8601 String đầu vào | `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")` |

---

## 2. Object Mapping Standard (Quy chuẩn Chuyển đổi DTO <-> Entity)

Hệ thống yêu cầu chuyển đổi dữ liệu tường minh (Explicit Mapping), đảm bảo hiệu năng cao và loại trừ hoàn toàn các lỗi tiềm ẩn do suy diễn ngầm (Implicit Magic).

### Nguyên tắc Lựa chọn Cơ chế Mapping

1. **Dự án Hiện tại (Scope vừa & nhỏ):** Sử dụng **Static Factory Methods** đặt trực tiếp tại các lớp DTO Response:
   ```java
   public record WorkOrderResponse(
       UUID id,
       String equipmentId,
       String description,
       Priority priority,
       WorkOrderStatus status,
       Instant createdAt,
       Instant resolvedAt
   ) {
       public static WorkOrderResponse from(final WorkOrder entity) {
           java.util.Objects.requireNonNull(entity, "workOrder must not be null");
           return new WorkOrderResponse(
               entity.getId(),
               entity.getEquipmentId(),
               entity.getDescription(),
               entity.getPriority(),
               entity.getStatus(),
               entity.getCreatedAt(),
               entity.getResolvedAt()
           );
       }
   }
   ```
2. **Khi Mở rộng Quy mô (Enterprise Scale):** Bắt buộc sử dụng **MapStruct** với cấu hình Spring Component (`componentModel = "spring"`).
3. **Chính sách Cấm:** **CẤM TUYỆT ĐỐI** sử dụng thư viện `ModelMapper` hoặc bất kỳ công cụ ánh xạ nào dựa trên Java Reflection Runtime do:
   - Làm chậm hiệu năng từ 5 đến 10 lần trong các đợt tải cao điểm.
   - Gây lỗi ngầm khi đổi tên trường (Refactor silent failure).
   - Làm rò rỉ dữ liệu ngoài ý muốn giữa Entity và DTO.

---

## 3. Standard Pagination & Sorting Schema (Quy chuẩn Phân trang)

Mọi API trả về danh sách tài nguyên (vd: `GET /api/v1/workorders`) bắt buộc phải hỗ trợ phân trang và sắp xếp có kiểm soát, ngăn ngừa tấn công làm cạn kiệt bộ nhớ (OOM Attack).

### Bảng Tham số Truy vấn Phân trang (Input Query Parameters)

| Tên Tham số | Kiểu Dữ liệu | Mặc định | Giới hạn (Boundary Constraints) | Ý nghĩa |
|---|---|---|---|---|
| `page` | `Integer` | `0` | $\ge 0$ | Chỉ số trang (0-indexed) |
| `size` | `Integer` | `20` | $1 \le \text{size} \le 100$ | Số lượng bản ghi tối đa mỗi trang |
| `sort` | `String` | `"createdAt,desc"` | Chỉ chấp nhận các trường: `createdAt`, `priority`, `status` | Tiêu chí sắp xếp, kèm hướng (`asc`/`desc`) |

### Cấu trúc Phản hồi Chuẩn: `PagedResponse<T>`

Toàn bộ response phân trang phải được bọc trong một Generic Record thống nhất:

| Tên Trường | Kiểu Dữ liệu | Ý nghĩa |
|---|---|---|
| `content` | `List<T>` | Danh sách các đối tượng dữ liệu của trang hiện tại |
| `pageNumber` | `Integer` | Chỉ số trang hiện tại (0-indexed) |
| `pageSize` | `Integer` | Kích thước trang thực tế |
| `totalElements` | `Long` | Tổng số bản ghi thỏa mãn điều kiện lọc trong toàn bộ DB |
| `totalPages` | `Integer` | Tổng số trang có thể truy cập |
| `isFirst` | `Boolean` | Có phải trang đầu tiên không |
| `isLast` | `Boolean` | Có phải trang cuối cùng không |

```java
// Schema định nghĩa PagedResponse dùng chung
package com.gpc.oms.dto;

import org.springframework.data.domain.Page;
import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean isFirst,
    boolean isLast
) {
    public static <T> PagedResponse<T> from(final Page<T> page) {
        java.util.Objects.requireNonNull(page, "page must not be null");
        return new PagedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
```

---

## 4. Custom Bean Validation & Constraints (Quy chuẩn Kiểm tra Tính hợp lệ)

Toàn bộ kiểm tra tính hợp lệ dữ liệu đầu vào bắt buộc thực hiện tại ranh giới API (Controller `@Valid`), không để lọt dữ liệu sai xuống tầng Service hoặc Database.

### Bảng Ràng buộc Đầu vào Chuẩn cho WorkOrder Request

| Ràng buộc | Annotation Áp dụng | Điều kiện Kiểm tra | Thông điệp Lỗi (RFC 7807 detail) |
|---|---|---|---|
| Mã thiết bị rỗng | `@NotBlank` | Không được null, độ dài chuỗi ký tự $> 0$ sau khi trim | `"equipmentId is required and must not be blank"` |
| Mã thiết bị quá dài | `@Size(max = 50)` | Độ dài tối đa 50 ký tự | `"equipmentId must not exceed 50 characters"` |
| Mô tả rỗng | `@NotBlank` | Không được để trống | `"description is required and must not be blank"` |
| Mô tả quá ngắn/dài | `@Size(min = 10, max = 500)` | Chiều dài từ 10 đến 500 ký tự | `"description must be between 10 and 500 characters"` |
| Mức độ ưu tiên sai | `@NotNull` | Phải thuộc Enum `Priority` | `"priority is required and must be LOW, MEDIUM, HIGH, or CRITICAL"` |

---

## 5. Resilience & Fault Tolerance (Khả năng Phục hồi & Chịu lỗi)

Khi hệ thống tích hợp với các dịch vụ bên ngoài (vd: Hệ thống GIS, Cổng gửi thông báo SMS/Zalo cho khách hàng sự cố), bắt buộc phải áp dụng quy tắc chịu lỗi:

### Nguyên tắc Thiết lập Cơ chế Retry (Thử lại)

1. **Chỉ Retry lỗi tạm thời (Transient Errors):**
   - Lỗi mất kết nối mạng tạm thời (`SocketTimeoutException`, `ConnectException`).
   - Lỗi DB Connection Pool tạm hết tài nguyên (`CannotAcquireLockException`).
2. **CẤM Retry lỗi nghiệp vụ:**
   - Cấm thử lại khi gặp lỗi 400 Bad Request (Validation failure).
   - Cấm thử lại khi gặp lỗi 422 Unprocessable Entity (Vi phạm State Machine).
   - Cấm thử lại khi gặp lỗi 404 Not Found hoặc 403 Forbidden.
3. **Cấu hình Exponential Backoff:**
   - **Số lần thử tối đa:** `3` lần.
   - **Khoảng trễ khởi điểm (Initial Interval):** `500ms`.
   - **Hệ số tăng (Multiplier):** `2.0` (Lần 1: 500ms, Lần 2: 1000ms, Lần 3: 2000ms).
   - **Độ trễ tối đa (Max Interval):** `3000ms`.

---

## 6. Modern Java 17 Idioms, JVM Performance & Code Reusability Guide

### 6.1. Modern Java 17 Syntax Idioms
1. **Compact Constructors for Records:** Sử dụng compact constructor `public RecordName { ... }` khi cần validate hoặc normalize dữ liệu đầu vào trong Record.
2. **Enhanced Switch Expressions:** Sử dụng cú pháp arrow `->` trả về giá trị trực tiếp, loại trừ hoàn toàn câu lệnh `break` và lỗi fall-through; bao quát 100% case mà không cần `default` khi switch trên enum đã đầy đủ.
3. **Java Text Blocks:** Sử dụng `"""` cho multi-line templates (JSON RFC 7807 fallback, SQL scripts, test payloads) thay vì phép cộng chuỗi `+`.
4. **Stream Pipelines Tối Ưu (`Stream.toList()`):** Bắt buộc dùng `.toList()` trực tiếp trên Stream thay vì `.collect(Collectors.toList())` hoặc `.collect(Collectors.toUnmodifiableList())`. Phương thức `.toList()` trả về unmodifiable list hiệu năng cao với chi phí cấp phát tối thiểu.
5. **Static Imports trong Kiểm Thử:** Đồng bộ 100% static imports cho assertions (`assertEquals`, `assertNotNull`, `assertThrows`, `assertThat`) và mocks (`when`, `verify`, `times`, `never`, `any`), nâng cao tỷ lệ tín hiệu trên nhiễu (Signal-to-Noise Ratio).

### 6.2. JVM & GC Performance Optimization
1. **Pre-sizing Collections:** Khi đã biết trước số lượng phần tử, bắt buộc khởi tạo với `initialCapacity` để triệt tiêu chi phí mảng co giãn (array resizing / copying):
   - `List`: `new ArrayList<>(fieldErrors.size())`
   - `Map`: `new HashMap<>((int) (expectedSize / 0.75f) + 1)`
2. **JIT Escape Analysis:** Đặt từ khóa `final` cho 100% method parameters và immutable local variables để hỗ trợ JIT C2 Compiler thực hiện Scalar Replacement và Stack Allocation.
3. **Fast-path Validation:** Sử dụng `Objects.requireNonNull(arg, "message")` tại đầu các constructors và public methods để bẫy lỗi sớm với chi phí CPU tối thiểu.
4. **Tránh Enum Array Cloning:** `Enum.values()` tạo ra một mảng clone mới sau mỗi lần gọi. Trong các hàm chuyển đổi hot-path, cache mảng tĩnh: `private static final WorkOrderStatus[] VALUES = WorkOrderStatus.values();`.

### 6.3. Code Reusability & DRY Principles
1. **Centralized Problem Types (`ProblemTypes.java`):** Gom toàn bộ các URI định danh lỗi RFC 7807 (`urn:problem-type:...`) thành hằng số `public static final URI` dùng chung, loại bỏ magic strings và tối ưu hóa thời gian parse URI:
   ```java
   public final class ProblemTypes {
       public static final URI VALIDATION_ERROR = URI.create("urn:problem-type:validation-error");
       public static final URI MALFORMED_JSON = URI.create("urn:problem-type:malformed-json");
       public static final URI UNAUTHORIZED = URI.create("urn:problem-type:unauthorized");
       public static final URI FORBIDDEN = URI.create("urn:problem-type:forbidden");
       public static final URI NOT_FOUND = URI.create("urn:problem-type:not-found");
       public static final URI INVALID_STATE_TRANSITION = URI.create("urn:problem-type:invalid-state-transition");
       public static final URI INTERNAL_ERROR = URI.create("urn:problem-type:internal-error");
   }
   ```
2. **Object Mother / Test Fixture Pattern (`WorkOrderTestFixtures.java`):** Tái sử dụng việc khởi tạo thực thể, DTO mẫu, và JSON request payload trong toàn bộ tầng kiểm thử Unit & Integration Tests, triệt tiêu mã boilerplate lặp lại:
   ```java
   public final class WorkOrderTestFixtures {
       public static final UUID DEFAULT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
       public static final String DEFAULT_EQUIPMENT_ID = "EQ-DEFAULT-01";
       public static final String DEFAULT_DESCRIPTION = "Standard line maintenance work order";
       public static final Priority DEFAULT_PRIORITY = Priority.HIGH;

       public static WorkOrder createDefaultEntity() {
           return new WorkOrder(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
       }
       public static WorkOrderRequest createDefaultRequest() {
           return new WorkOrderRequest(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY);
       }
       public static String createDefaultRequestJson() {
           return """
               {
                 "equipmentId": "%s",
                 "description": "%s",
                 "priority": "%s"
               }
               """.formatted(DEFAULT_EQUIPMENT_ID, DEFAULT_DESCRIPTION, DEFAULT_PRIORITY.name());
       }
   }
   ```

### 6.4. Zero Magic Numbers & Zero Literal Strings Policy
Hệ thống áp dụng triệt để nguyên tắc **Zero Magic Values (Joshua Bloch Item 68)**:

1. **Quy định về Centralized Constants:**
   - **Tên Metrics & Tag Keys:** Bắt buộc định nghĩa trong `WorkOrderMetrics.java`:
     ```java
     public final class WorkOrderMetrics {
         public static final String COUNTER_CREATED = "oms_workorders_created_total";
         public static final String COUNTER_TRANSITIONS = "oms_workorder_status_transitions_total";
         public static final String TAG_FROM_STATUS = "from_status";
         public static final String TAG_TO_STATUS = "to_status";
         public static final String TAG_PRIORITY = "priority";
         public static final String TAG_STATUS = "status";
         private WorkOrderMetrics() {}
     }
     ```
   - **Quyền Hạn & Vai Trò (RBAC):** Bắt buộc định nghĩa trong `RoleConstants.java`:
     ```java
     public final class RoleConstants {
         public static final String ROLE_ADMIN = "ROLE_ADMIN";
         public static final String ROLE_DISPATCHER = "ROLE_DISPATCHER";
         public static final String ROLE_TECHNICIAN = "ROLE_TECHNICIAN";
         public static final String ROLE_ANONYMOUS = "ROLE_ANONYMOUS";
         public static final String HAS_ROLE_ADMIN_OR_DISPATCHER = 
             "hasAnyRole('" + ROLE_ADMIN + "', '" + ROLE_DISPATCHER + "')";
         private RoleConstants() {}
     }
     ```
   - **RFC 7807 Error Keys & Problem Types:** Bắt buộc định nghĩa trong `ProblemTypes.java`:
     ```java
     public static final String KEY_INVALID_PARAMS = "invalidParams";
     public static final String KEY_NAME = "name";
     public static final String KEY_REASON = "reason";
     public static final String TITLE_VALIDATION_FAILED = "Validation Failed";
     ```
   - **Ràng Buộc Độ Dài Thực Thể & DTO:** Dùng chung hằng số công khai để chống lệch pha:
     ```java
     public static final int MAX_EQUIPMENT_ID_LENGTH = 50;
     public static final int MIN_DESCRIPTION_LENGTH = 10;
     public static final int MAX_DESCRIPTION_LENGTH = 500;
     ```

2. **Quy định về Configuration Properties (`@ConfigurationProperties`):**
   - Mọi tham số số học về Rate Limiting (giới hạn request, cửa sổ thời gian, dung lượng cache) hoặc timeouts phải được externalize vào `application.yml`:
     ```yaml
     oms:
       rate-limit:
         max-requests-per-minute: 60
         window-minutes: 10
         cache-max-size: 10000
     ```
   - Đọc qua Spring `@ConfigurationProperties(prefix = "oms.rate-limit")` thay vì hardcode trong Filter.

3. **Quy định về Media Types & HTTP Headers:**
   - Cấm viết chuỗi thô `"application/problem+json"` $\rightarrow$ dùng `MediaType.APPLICATION_PROBLEM_JSON_VALUE`.
   - Cấm viết chuỗi thô `"Retry-After"` $\rightarrow$ dùng `HttpHeaders.RETRY_AFTER`.
4. **Quy định về Khoảng Trống Trước Lệnh Return (Blank Line Before Return Statement):**
   - **Quy tắc cốt lõi:** Trong mọi phương thức (method) hoặc khối mã lệnh có từ 2 câu lệnh trở lên, câu lệnh `return` bắt buộc phải được ngăn cách với các dòng mã xử lý logic phía trên bằng **chính xác 1 dòng trống (blank line)**.
   - **Mục đích:** Tách biệt rõ ràng giai đoạn xử lý/tính toán dữ liệu với thời điểm thoát hàm (execution boundary), tạo ranh giới thị giác rõ ràng giúp nâng cao tính trực quan và khả năng đọc mã (readability) cho kỹ sư cũng như AI review.
   - **Ngoại lệ hợp lệ:**
     * Phương thức chỉ chứa duy nhất một câu lệnh bên trong thân hàm (Single-line body statement, ví dụ getter đơn giản hoặc delegate trực tiếp `return service.call();`) thì không cần dòng trống phía trên.
     * Khối lệnh ngắn trong `case -> return ...;` hoặc lambda expression 1 dòng.
   - **Minh họa quy chuẩn:**
     ```java
     // ❌ BAD: Không có dòng trống ngăn cách trước lệnh return
     final WorkOrderResponse response = workOrderService.createWorkOrder(request);
     final URI location = URI.create(PATH_WORKORDERS + "/" + response.id());
     return ResponseEntity.created(location).body(response);

     // ✅ GOOD: Cách 1 dòng trống trước lệnh return
     final WorkOrderResponse response = workOrderService.createWorkOrder(request);
     final URI location = URI.create(PATH_WORKORDERS + "/" + response.id());

     return ResponseEntity.created(location).body(response);
     ```

