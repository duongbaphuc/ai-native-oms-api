<!--
Role: Principal Software Architect & Standards Lead
Task: Define internal coding conventions, date/time timezone policy, object mapping rules, pagination standards, and resilience patterns
Context files: docs/coding-rules.md, docs/api-rules.md, docs/domain-model.md, docs/api-spec.md
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
       public static WorkOrderResponse from(WorkOrder entity) {
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
    public static <T> PagedResponse<T> from(Page<T> page) {
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
