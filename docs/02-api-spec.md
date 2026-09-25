<!--
Role: Principal API Architect
Task: API Contract, Request/Response Table Schemas, RBAC, Step-by-step Logic, and RFC 7807 Error Mappings
Context files: docs/01-domain-model.md, docs/00-api-rules.md, docs/00-security-rules.md, docs/00-internal-coding-standards.md
Constraints: REST plural `/api/v1/workorders`, RFC 7807 problem+json, Strict Bean Validation, 3-tier architecture
Target Files:
- src/main/java/com/gpc/oms/controller/WorkOrderController.java
- src/main/java/com/gpc/oms/dto/WorkOrderRequest.java
- src/main/java/com/gpc/oms/dto/WorkOrderStatusRequest.java
- src/main/java/com/gpc/oms/dto/WorkOrderResponse.java
- src/main/java/com/gpc/oms/dto/PagedResponse.java
- src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java
-->
# Đặc tả Hợp đồng API (API Specification)

Tài liệu này xác định các giao ước RESTful API chính thức của phân hệ Outage Work Order. Toàn bộ request/response bắt buộc sử định dạng JSON UTF-8 và tuân thủ chuẩn lỗi **RFC 7807 Problem Details**.

---

## 1. POST `/api/v1/workorders` — Tạo Mới Phiếu Sự Cố

- **Phân quyền (RBAC):** Bắt buộc role `DISPATCHER`, `TECHNICIAN`, hoặc `ADMIN` (`@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")`).
- **Target Handler:** `WorkOrderController.createWorkOrder(@Valid @RequestBody WorkOrderRequest request)`

### Bảng Schema Request Body (`WorkOrderRequest`)

| Tên Trường | Kiểu Dữ liệu | Bắt buộc | Ràng buộc Validation | Mô tả |
|---|---|---|---|---|
| `equipmentId` | `String` | Có | `@NotBlank, @Size(max = 50)` | Mã định danh thiết bị lưới điện (Trạm, Recloser) |
| `description` | `String` | Có | `@NotBlank, @Size(min = 10, max = 500)` | Nội dung mô tả chi tiết sự cố |
| `priority` | `Priority` | Có | `@NotNull` | Mức ưu tiên: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |

### Bảng Schema Response Body (`WorkOrderResponse` - HTTP 201 Created)

| Tên Trường | Kiểu Dữ liệu | Nullable | Định dạng | Mô tả |
|---|---|---|---|---|
| `id` | `UUID` | Không | UUID v4 chuẩn | Mã định danh tự sinh của Work Order |
| `equipmentId` | `String` | Không | Chuỗi ký tự | Mã thiết bị lưới điện |
| `description` | `String` | Không | Chuỗi ký tự | Nội dung mô tả sự cố |
| `priority` | `Priority` | Không | Enum (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) | Mức độ ưu tiên |
| `status` | `WorkOrderStatus` | Không | Luôn là `"OPEN"` khi mới tạo | Trạng thái khởi tạo |
| `createdAt` | `Instant` | Không | ISO-8601 UTC (`yyyy-MM-dd'T'HH:mm:ssXXX`) | Thời điểm tiếp nhận |
| `resolvedAt` | `Instant` | **Có** | `null` | Chưa khắc phục xong |

### Thuật toán Xử lý (Step-by-step Logic)

1. Controller nhận `WorkOrderRequest` và kích hoạt Bean Validation tự động (`@Valid`).
2. Controller ủy quyền cho `WorkOrderService.createWorkOrder(request)`.
3. Service khởi tạo thực thể `WorkOrder` mới với trạng thái ban đầu là `OPEN` và `createdAt = Instant.now()`.
4. Service lưu vào cơ sở dữ liệu qua `WorkOrderRepository.save(workOrder)`.
5. Service chuyển đổi thực thể sang `WorkOrderResponse` qua static factory method `WorkOrderResponse.from(saved)`.
6. Controller trả về HTTP `201 Created` cùng header `Location: /api/v1/workorders/{id}`.

### Ma trận Xử lý Ngoại lệ (Edge Cases)

| Điều kiện Vi phạm | HTTP Status | RFC 7807 `type` | RFC 7807 `detail` |
|---|---|---|---|
| Thiếu trường hoặc vi phạm độ dài | `400 Bad Request` | `urn:problem-type:validation-error` | `"Validation failed for field: {fieldName}"` |
| JSON sai cú pháp hoặc sai Enum | `400 Bad Request` | `urn:problem-type:malformed-json` | `"JSON request body is malformed or invalid"` |
| Không truyền Token xác thực | `401 Unauthorized` | `urn:problem-type:unauthorized` | `"Authentication token is missing or expired"` |
| Token có role không được phép (vd: `ROLE_GUEST`) | `403 Forbidden` | `urn:problem-type:forbidden` | `"Access Denied: Insufficient permissions"` |

---

## 2. GET `/api/v1/workorders` — Lấy Danh Sách Phiếu Sự Cố

- **Phân quyền (RBAC):** Bắt buộc role `DISPATCHER`, `TECHNICIAN`, hoặc `ADMIN`.
- **Target Handler:** `WorkOrderController.getWorkOrders(Pageable pageable, @RequestParam(required = false) WorkOrderStatus status)`

### Bảng Tham số Truy vấn (Query Parameters)

| Tên Tham số | Kiểu Dữ liệu | Bắt buộc | Mặc định | Mô tả |
|---|---|---|---|---|
| `page` | `Integer` | Không | `0` | Chỉ số trang bắt đầu từ 0 |
| `size` | `Integer` | Không | `20` | Số lượng bản ghi tối đa mỗi trang (max 100) |
| `status` | `WorkOrderStatus` | Không | `null` | Lọc theo trạng thái (`OPEN`, `IN_PROGRESS`, `DONE`) |

### Bảng Schema Phản hồi Thành công (`PagedResponse<WorkOrderResponse>` - HTTP 200 OK)

| Tên Trường | Kiểu Dữ liệu | Nullable | Mô tả |
|---|---|---|---|
| `content` | `List<WorkOrderResponse>` | Không | Danh sách các phiếu sự cố trong trang hiện tại |
| `pageNumber` | `Integer` | Không | Chỉ số trang hiện tại (0-indexed) |
| `pageSize` | `Integer` | Không | Số lượng phần tử tối đa trên mỗi trang |
| `totalElements` | `Long` | Không | Tổng số bản ghi thỏa mãn điều kiện lọc |
| `totalPages` | `Integer` | Không | Tổng số trang |
| `isFirst` | `Boolean` | Không | `true` nếu là trang đầu tiên |
| `isLast` | `Boolean` | Không | `true` nếu là trang cuối cùng |

---

## 3. GET `/api/v1/workorders/{id}` — Lấy Chi Tiết Phiếu Sự Cố

- **Phân quyền (RBAC):** Bắt buộc role `DISPATCHER`, `TECHNICIAN`, hoặc `ADMIN`.
- **Target Handler:** `WorkOrderController.getWorkOrderById(@PathVariable UUID id)`

### Thuật toán Xử lý (Step-by-step Logic)

1. Controller nhận `id` từ path variable.
2. Controller gọi `WorkOrderService.getWorkOrderById(id)`.
3. Service tìm kiếm trong CSDL qua `WorkOrderRepository.findById(id)`.
4. Nếu không tìm thấy: ném `ResourceNotFoundException("WorkOrder not found with id: " + id)`.
5. Nếu tìm thấy: chuyển đổi sang `WorkOrderResponse` và trả về HTTP `200 OK`.

### Ma trận Xử lý Ngoại lệ (Edge Cases)

| Điều kiện Vi phạm | HTTP Status | RFC 7807 `type` | RFC 7807 `detail` |
|---|---|---|---|
| Không tìm thấy ID trong CSDL | `404 Not Found` | `urn:problem-type:not-found` | `"WorkOrder not found with id: {id}"` |
| Định dạng ID không phải UUID | `400 Bad Request` | `urn:problem-type:validation-error` | `"Validation Failed"` (kèm `invalidParams: [{"name": "id", "reason": "Invalid value for parameter 'id'"}]`) |

---

## 4. PATCH `/api/v1/workorders/{id}/status` — Cập Nhật Trạng Thái

- **Phân quyền (RBAC):** Bắt buộc role `TECHNICIAN` hoặc `ADMIN`.
- **Target Handler:** `WorkOrderController.updateStatus(@PathVariable UUID id, @Valid @RequestBody WorkOrderStatusRequest request)`

### Bảng Schema Request Body (`WorkOrderStatusRequest`)

| Tên Trường | Kiểu Dữ liệu | Bắt buộc | Ràng buộc Validation | Mô tả |
|---|---|---|---|---|
| `status` | `WorkOrderStatus` | Có | `@NotNull` | Trạng thái đích muốn chuyển: `IN_PROGRESS` hoặc `DONE` |

### Thuật toán Xử lý (Step-by-step Logic)

1. Controller nhận `id` và payload `WorkOrderStatusRequest`.
2. Controller ủy quyền cho `WorkOrderService.advanceStatus(id, request.status())`.
3. Service tìm kiếm `WorkOrder` theo `id`. Nếu không thấy $\rightarrow$ ném `ResourceNotFoundException`.
4. Service gọi `workOrder.advanceStatus(newStatus)` trên Entity.
5. Entity kiểm tra `canTransitionTo()`. Nếu vi phạm $\rightarrow$ ném `IllegalStateException`.
6. Nếu hợp lệ: Cập nhật trạng thái (và gán `resolvedAt = Instant.now()` nếu `DONE`).
7. Service lưu vào CSDL và trả về `WorkOrderResponse` với HTTP `200 OK`.

### Ma trận Xử lý Ngoại lệ (Edge Cases)

| Điều kiện Vi phạm | HTTP Status | RFC 7807 `type` | RFC 7807 `detail` |
|---|---|---|---|
| Vi phạm thứ tự máy trạng thái (vd: `DONE` -> `OPEN`) | `422 Unprocessable Entity` | `urn:problem-type:invalid-state-transition` | `"Invalid state transition from {current} to {target}"` |
| Không tìm thấy ID trong CSDL | `404 Not Found` | `urn:problem-type:not-found` | `"WorkOrder not found with id: {id}"` |
| Role `DISPATCHER` gọi API này | `403 Forbidden` | `urn:problem-type:forbidden` | `"Access Denied"` |

---

## 5. Cấu trúc Lỗi Chuẩn RFC 7807 Problem Details

Mọi lỗi trả về client bắt buộc tuân thủ schema JSON sau (`application/problem+json`):

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation Failed",
  "instance": "/api/v1/workorders",
  "invalidParams": [
    {
      "name": "equipmentId",
      "reason": "must not be blank"
    }
  ]
}
```

### Danh Mục URN Lỗi Hệ Thống (RFC 7807 Error Catalog)

| HTTP Status | Problem Type URN | Title Mặc định | Handler Phụ trách | Kịch bản Kích hoạt |
|---|---|---|---|---|
| `400 Bad Request` | `urn:problem-type:validation-error` | `Validation Failed` | `handleValidationErrors` | Vi phạm `@Valid` (@NotBlank, @NotNull, @Size) trên Request Body |
| `400 Bad Request` | `urn:problem-type:malformed-json` | `Malformed Request Body` | `handleMalformedJson` | JSON sai cú pháp, enum không hợp lệ, hoặc parse error |
| `400 Bad Request` | `urn:problem-type:validation-error` | `Validation Failed` | `handleQueryParamTypeMismatch` | Query param hoặc Path variable sai kiểu dữ liệu |
| `401 Unauthorized` | `urn:problem-type:unauthorized` | `Unauthorized` | `CustomAuthenticationEntryPoint` | Thiếu hoặc sai thông tin xác thực HTTP Basic |
| `403 Forbidden` | `urn:problem-type:forbidden` | `Access Denied` | `handleAccessDenied` | Vi phạm phân quyền RBAC (`@PreAuthorize`) |
| `404 Not Found` | `urn:problem-type:not-found` | *Message chi tiết* | `handleResourceNotFound` | Không tìm thấy bản ghi theo UUID chỉ định |
| `422 Unprocessable Entity` | `urn:problem-type:invalid-state-transition` | *Message chi tiết* | `handleIllegalStateTransition` | Vi phạm quy tắc chuyển trạng thái của State Machine |
| `429 Too Many Requests` | `urn:problem-type:rate-limit-exceeded` | `Too Many Requests` | `RateLimitingFilter` | Vượt ngưỡng tần suất gọi (10 write / 60 read req/min per IP) |
| `500 Internal Server Error` | `urn:problem-type:internal-error` | `An unexpected error occurred` | `handleUnexpected` | Lỗi ngoại lệ không lường trước (che giấu stack trace) |

---

## 6. Giám Sát & Thăm Dò Sức Khỏe Ứng Dụng (Spring Boot Actuator Probes)

Hệ thống cung cấp các endpoint thăm dò trạng thái phục vụ giám sát container, Kubernetes liveness/readiness probes và bộ điều phối CI/CD:

| Endpoint | Giao thức | Phân Quyền | Mục Đích Sử Dụng | Phản Hồi Thành Công |
|---|---|---|---|---|
| `/actuator/health` | `GET` | Public (`permitAll`) | Kiểm tra sức khỏe tổng thể và Kubernetes Liveness/Readiness probes | `200 OK` `{"status":"UP"}` |
| `/actuator/info` | `GET` | Public (`permitAll`) | Cung cấp thông tin phiên bản và build metadata của ứng dụng | `200 OK` `{}` |
| `/actuator/prometheus` | `GET` | `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")`) | Thu thập số liệu đo lường Micrometer Prometheus cho SRE/Grafana | `200 OK` (Text format) |

> [!NOTE]
> Thuộc tính `management.endpoint.health.show-details: when-authorized` đảm bảo chi tiết thành phần nội bộ (DB, disk) chỉ hiển thị khi có chứng thực hợp lệ, ngăn chặn rò rỉ cấu trúc hạ tầng ra bên ngoài.

---

## 7. Giao Thức Quản Lý Header & Truy Vết Phân Tán (Headers & Distributed Tracing)

- **Header `X-Correlation-Id`:** Client có thể chủ động gửi mã truy vết trong request. Nếu request không có header này, `CorrelationIdFilter` tự động sinh một UUID v4 ngẫu nhiên, đưa vào SLF4J MDC context (`traceId`) và luôn luôn trả về header `X-Correlation-Id` trong 100% response.
- **Header `Location`:** Trả về khi tạo mới phiếu thành công (HTTP 201 Created), trỏ đến URI của tài nguyên vừa tạo: `/api/v1/workorders/{id}`.
