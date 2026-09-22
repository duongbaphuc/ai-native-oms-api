# Khung Kiến trúc Nghiệp vụ (Domain Model)

Tài liệu này xác định các thực thể và quy tắc bất biến nhằm giữ cho mô hình dữ liệu nhẹ gọn và giới hạn ranh giới sinh mã của AI[cite: 4].

## Thực thể (Entities)

### `WorkOrder` (Phiếu Sự Cố)
Đại diện cho một sự kiện mất điện cần được xử lý.
- `id` (UUID): Định danh hệ thống.
- `equipmentId` (String): Mã định danh thiết bị lưới điện[cite: 4].
- `priority` (Enum): `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`[cite: 4].
- `status` (String): Trạng thái hiện tại của phiếu[cite: 4].
- `createdAt` (DateTime): Thời điểm ghi nhận[cite: 4].

## Quy tắc Bất biến (Invariants)

> [!IMPORTANT]
> **Máy trạng thái đơn hướng (One-way State Machine):** Thuộc tính `status` chỉ được phép chuyển đổi theo luồng tuyến tính: `Open` -> `InProgress` -> `Done`[cite: 4]. Mã nguồn hệ thống (và AI) tuyệt đối không được phép hỗ trợ việc lùi trạng thái.
