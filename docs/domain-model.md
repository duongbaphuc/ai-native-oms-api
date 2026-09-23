# Khung Kiến trúc Nghiệp vụ (Domain Model)

Tài liệu này xác định các thực thể và quy tắc bất biến nhằm giữ cho mô hình dữ liệu nhẹ gọn và giới hạn ranh giới sinh mã của AI.

## Thực thể (Entities)

### `WorkOrder` (Phiếu Sự Cố)
Đại diện cho một sự kiện mất điện cần được xử lý.
- `id` (UUID): Định danh hệ thống.
- `equipmentId` (String): Mã định danh thiết bị lưới điện.
- `description` (String): Mô tả chi tiết về sự kiện mất điện.
- `priority` (Enum): `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- `status` (Enum): Trạng thái hiện tại của phiếu — `Open`, `InProgress`, `Done`.
- `createdAt` (DateTime): Thời điểm ghi nhận.
- `resolvedAt` (DateTime, nullable): Thời điểm khắc phục xong. Tự động gán khi chuyển sang `Done`.

## Quy tắc Bất biến (Invariants)

> [!IMPORTANT]
> **Máy trạng thái đơn hướng (One-way State Machine):** Thuộc tính `status` chỉ được phép chuyển đổi theo luồng tuyến tính: `Open` -> `InProgress` -> `Done`. Mã nguồn hệ thống (và AI) tuyệt đối không được phép hỗ trợ việc lùi trạng thái.

> [!NOTE]
> **Gán tự động `resolvedAt`:** Khi `status` chuyển sang `Done`, hệ thống tự động gán `resolvedAt` = thời điểm hiện tại. Giá trị này dùng để tính toán chỉ số SAIDI/SAIFI.
