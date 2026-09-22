# Đặc tả Hợp đồng API (API Specification)

Toàn bộ AI prompt phải trích dẫn trực tiếp các đường dẫn (paths) trong tài liệu này sau khi được phê duyệt[cite: 4].

## 1. Tạo Phiếu Sự Cố Mới
- **Đường dẫn (Path):** `POST /api/v1/workorders`
- **Yêu cầu (Request):**
  ```json
  {
    "equipmentId": "EQ-77",
    "priority": "HIGH"
  }
  ```
- **Phản hồi Thành công (201 Created):**
  ```json
  {
    "id": "WO-10432",
    "equipmentId": "EQ-77",
    "priority": "HIGH",
    "status": "Open",
    "createdAt": "2026-08-30T09:15:00Z"
  }
  ```

> [!WARNING]
> Mọi lỗi xác thực dữ liệu đầu vào (HTTP 400) phải tuân thủ nghiêm ngặt định dạng RFC 7807 (Problem Details).
