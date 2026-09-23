# Đặc tả Hợp đồng API (API Specification)

Toàn bộ AI prompt phải trích dẫn trực tiếp các đường dẫn (paths) trong tài liệu này sau khi được phê duyệt.

## 1. Tạo Phiếu Sự Cố Mới
- **Đường dẫn (Path):** `POST /api/v1/workorders`
- **Phân quyền:** `hasRole('TECHNICIAN')` hoặc `hasRole('DISPATCHER')`
- **Yêu cầu (Request):**
  ```json
  {
    "equipmentId": "EQ-77",
    "description": "Máy biến áp T3 khu vực Bình Thạnh bị quá tải, mất điện toàn bộ tuyến",
    "priority": "HIGH"
  }
  ```
- **Phản hồi Thành công (201 Created):**
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "equipmentId": "EQ-77",
    "description": "Máy biến áp T3 khu vực Bình Thạnh bị quá tải, mất điện toàn bộ tuyến",
    "priority": "HIGH",
    "status": "Open",
    "createdAt": "2026-08-30T09:15:00Z",
    "resolvedAt": null
  }
  ```

## 2. Lấy Danh sách Phiếu Sự Cố
- **Đường dẫn (Path):** `GET /api/v1/workorders`
- **Phân quyền:** `hasRole('DISPATCHER')`
- **Phản hồi Thành công (200 OK):**
  ```json
  [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "equipmentId": "EQ-77",
      "description": "Máy biến áp T3 khu vực Bình Thạnh bị quá tải",
      "priority": "HIGH",
      "status": "Open",
      "createdAt": "2026-08-30T09:15:00Z",
      "resolvedAt": null
    }
  ]
  ```

## 3. Lấy Chi tiết Phiếu Sự Cố
- **Đường dẫn (Path):** `GET /api/v1/workorders/{id}`
- **Phân quyền:** `hasRole('TECHNICIAN')` hoặc `hasRole('DISPATCHER')`
- **Phản hồi Thành công (200 OK):** Trả về object đơn lẻ có cấu trúc giống mục 1.
- **Phản hồi Lỗi (404 Not Found):** Xem mục 5 — RFC 7807 Error Response.

## 4. Cập nhật Trạng thái Phiếu
- **Đường dẫn (Path):** `PATCH /api/v1/workorders/{id}/status`
- **Phân quyền:** `hasRole('TECHNICIAN')` hoặc `hasRole('DISPATCHER')`
- **Yêu cầu (Request):**
  ```json
  {
    "status": "InProgress"
  }
  ```
- **Phản hồi Thành công (200 OK):** Trả về object đã cập nhật trạng thái.

> [!IMPORTANT]
> Trạng thái chỉ được chuyển đổi theo luồng tuyến tính: `Open` -> `InProgress` -> `Done`. Khi chuyển sang `Done`, hệ thống tự động gán `resolvedAt` = thời điểm hiện tại.

## 5. Chuẩn Lỗi RFC 7807 (Error Responses)

> [!WARNING]
> Mọi lỗi xác thực dữ liệu đầu vào (HTTP 400) phải tuân thủ nghiêm ngặt định dạng RFC 7807 (Problem Details).

- **Lỗi Xác thực (400 Bad Request):**
  ```json
  {
    "type": "https://api.oms.gpc.com/errors/validation",
    "title": "Validation Failed",
    "status": 400,
    "invalidParams": [
      {
        "name": "priority",
        "reason": "must be one of: LOW, MEDIUM, HIGH, CRITICAL"
      }
    ]
  }
  ```
- **Không tìm thấy (404 Not Found):**
  ```json
  {
    "type": "https://api.oms.gpc.com/errors/not-found",
    "title": "Work Order Not Found",
    "status": 404
  }
  ```
- **Không có quyền (403 Forbidden):**
  ```json
  {
    "type": "https://api.oms.gpc.com/errors/forbidden",
    "title": "Access Denied",
    "status": 403
  }
  ```
- **Chuyển trạng thái không hợp lệ (422 Unprocessable Entity):**
  ```json
  {
    "type": "https://api.oms.gpc.com/errors/invalid-state-transition",
    "title": "Invalid State Transition",
    "status": 422
  }
  ```
