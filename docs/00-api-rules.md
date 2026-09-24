<!--
Role: Principal API Architect
Task: API Design Rules, RFC 7807 Standard, and Jackson Strict Schema Configuration
Context files: docs/00-coding-rules.md, docs/00-security-rules.md, docs/02-api-spec.md
Constraints: REST plural resources, RFC 7807 problem+json, Strict schema (fail on unknown properties)
Target Files:
- src/main/java/com/gpc/oms/controller/WorkOrderController.java
- src/main/java/com/gpc/oms/dto/WorkOrderRequest.java
- src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java
-->
# Tiêu chuẩn Thiết kế API (API Design Rules)

Tài liệu này xác định các quy tắc thiết kế giao diện lập trình ứng dụng (RESTful API Design Standards) áp dụng đồng bộ cho toàn bộ microservices thuộc phân hệ OMS.

---

## 1. Định Tuyến & Tài Nguyên (Resource Naming)

1. **Danh từ Số nhiều:** Toàn bộ REST endpoints phải sử dụng danh từ số nhiều ở dạng chữ thường (vd: `/api/v1/workorders`).
2. **Phiên bản Hóa:** Bắt buộc đặt phiên bản tiền tố `/api/v1/` trong đường dẫn.
3. **Cấm Phát minh Trường:** AI Copilot không được phép tự thêm các trường JSON ngoài phạm vi đã được quy định trong `docs/02-api-spec.md`.

---

## 2. Tiêu chuẩn Schema Nghiêm Ngặt (Strict Schema Validation)

1. **Từ chối Thuộc tính Lạ (Fail on Unknown Properties):**
   Mọi DTO Request bắt buộc cấu hình chặn các trường thừa không mong muốn để phòng chống tấn công Mass Assignment:
   ```java
   @JsonIgnoreProperties(ignoreUnknown = false)
   public record WorkOrderRequest( ... ) {}
   ```
2. **Bean Validation tại Boundary:** Mọi tham số `@RequestBody` ở Controller bắt buộc có annotation `@Valid`. Toàn bộ trường dữ liệu phải có ràng buộc rõ ràng (`@NotBlank`, `@NotNull`, `@Size`).

---

## 3. Tiêu chuẩn Lỗi Chuẩn Hóa RFC 7807 (Problem Details)

Toàn bộ response lỗi (HTTP 4xx, 5xx) bắt buộc trả về Content-Type: `application/problem+json`.

### Bảng Cấu trúc Trường Lỗi RFC 7807

| Tên Trường | Kiểu Dữ liệu | Bắt buộc | Mô tả |
|---|---|---|---|
| `type` | `URI` | Có | Định danh loại lỗi (vd: `urn:problem-type:validation-error`) |
| `title` | `String` | Có | Tóm tắt ngắn gọn loại lỗi bằng tiếng Anh |
| `status` | `Integer` | Có | Mã trạng thái HTTP (vd: 400, 401, 403, 404, 422, 500) |
| `detail` | `String` | Có | Thông điệp lỗi chi tiết giải thích nguyên nhân |
| `instance` | `String` | Có | URI của request gây ra lỗi (vd: `/api/v1/workorders`) |
| `invalidParams` | `List<InvalidParam>` | Khi lỗi 400 | Danh sách chi tiết các trường bị vi phạm validation (`name`, `reason`) |

### Danh Mục Loại Lỗi (Error Types Catalog)

| HTTP Status | Chuẩn URI RFC 7807 | Tiêu đề (`title`) | Khi nào kích hoạt? |
|---|---|---|---|
| `400 Bad Request` | `urn:problem-type:validation-error` | Validation Failed | Vi phạm `@NotBlank`, `@Size`, `@NotNull` trên trường cụ thể |
| `400 Bad Request` | `urn:problem-type:malformed-json` | Malformed JSON | JSON sai cú pháp, giá trị Enum không tồn tại, field lạ |
| `401 Unauthorized` | `urn:problem-type:unauthorized` | Unauthorized | Thiếu Token xác thực hoặc Token đã hết hạn |
| `403 Forbidden` | `urn:problem-type:forbidden` | Access Denied | Người dùng không đủ quyền hạn (Role) để gọi endpoint |
| `404 Not Found` | `urn:problem-type:not-found` | Not Found | Không tìm thấy tài nguyên với ID tương ứng trong CSDL |
| `422 Unprocessable Entity`| `urn:problem-type:invalid-state-transition`| Invalid State Transition | Vi phạm quy tắc máy trạng thái đơn hướng |
| `500 Internal Server Error`| `urn:problem-type:internal-error` | Internal Server Error | Lỗi hệ thống ngoài dự kiến |
