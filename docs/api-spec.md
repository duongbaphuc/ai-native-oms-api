<!--
Role: Principal API Architect
Task: API Contract, Request/Response Table Schemas, RBAC, Step-by-step Logic, and RFC 7807 Error Mappings
Context files: docs/domain-model.md, docs/api-rules.md, docs/security-rules.md, docs/internal-coding-standards.md
Constraints: REST plural `/api/v1/workorders`, RFC 7807 problem+json, Strict Bean Validation, 3-tier architecture
Target Files:
- src/main/java/com/gpc/oms/controller/WorkOrderController.java
- src/main/java/com/gpc/oms/dto/WorkOrderRequest.java
- src/main/java/com/gpc/oms/dto/WorkOrderStatusRequest.java
- src/main/java/com/gpc/oms/dto/WorkOrderResponse.java
- src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java
-->
# Đặc tả Hợp đồng API (API Specification)

Tài liệu này xác định các giao ước RESTful API chính thức của phân hệ Outage Work Order. Toàn bộ request/response bắt buộc sử dụng định dạng JSON UTF-8 và tuân thủ chuẩn lỗi **RFC 7807 Problem Details**.

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

### Phản hồi Thành công (HTTP 200 OK)

Trả về mảng danh sách `List<WorkOrderResponse>` hoặc đối tượng phân trang `PagedResponse<WorkOrderResponse>`.

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
| Định dạng ID không phải UUID | `400 Bad Request` | `urn:problem-type:type-mismatch` | `"Parameter 'id' must be a valid UUID"` |

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
| Role `DISPATCHER` gọi API này | `403 Forbidden` | `urn:problem-type:forbidden` | `"Access Denied: Only TECHNICIAN can update status"` |

---

## 5. Cấu trúc Lỗi Chuẩn RFC 7807 Problem Details

Mọi lỗi trả về client bắt buộc tuân thủ schema JSON sau:

```json
{
  "type": "urn:problem-type:validation-error",
  "title": "Validation Failed",
  "status": 400,
  "detail": "Validation failed for field: equipmentId",
  "instance": "/api/v1/workorders",
  "invalidParams": [
    {
      "name": "equipmentId",
      "reason": "must not be blank"
    }
  ]
}
```
