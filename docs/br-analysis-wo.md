# Phân tích Yêu cầu Nghiệp vụ (BR Analysis) - Outage Work Order

Dịch vụ Outage Work Order là một microservice cốt lõi thuộc phân hệ Outage Management System (OMS)[cite: 6]. API này cung cấp các giao thức RESTful để tạo, quản lý và theo dõi vòng đời của các sự kiện mất điện trên lưới điện. Tài liệu này bóc tách yêu cầu thô từ Product Owner (PO) thành các đặc tả kỹ thuật có thể thực thi.

## Yêu cầu thô ban đầu (Raw Business Requirement)

> "Bộ phận Vận hành Lưới điện cần một tính năng mới trên ứng dụng để điều độ viên và thợ hiện trường báo cáo các sự kiện mất điện (Outage). Người dùng cần nhập mã thiết bị (equipment_id) bị lỗi, mô tả sự cố và đánh giá mức độ nghiêm trọng (priority). Hệ thống phải lưu lại toàn bộ vòng đời xử lý sự cố (từ lúc tạo đến lúc đóng phiếu). Dữ liệu này phải được đồng bộ liên tục về Nền tảng Dữ liệu trung tâm để phục vụ tính toán các chỉ số tin cậy cung cấp điện như SAIDI, SAIFI."

---

## 1. Danh sách Thực thể (Entities & Properties)

Dựa trên yêu cầu nghiệp vụ, hệ thống OMS quản lý thực thể chính sau[cite: 6, 7]:

**Thực thể `OutageWorkOrder`:**
- `id` (UUID): Định danh duy nhất của phiếu sự cố.
- `equipment_id` (String): Mã định danh thiết bị lưới điện (ví dụ: máy biến áp, đường dây).
- `description` (String): Mô tả chi tiết về sự kiện mất điện.
- `priority` (Enum): Mức độ nghiêm trọng (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`)[cite: 4].
- `status` (Enum): Vòng đời xử lý (`Open` -> `InProgress` -> `Done`)[cite: 4].
- `created_at` (Timestamp): Thời điểm ghi nhận sự cố.
- `resolved_at` (Timestamp): Thời điểm khắc phục xong (dùng để tính toán thời gian mất điện).

---

## 2. Rủi ro Nghiệp vụ & Câu hỏi Mở (Open Questions & Risks)

> [!WARNING]
> **Rủi ro Đồng bộ Dữ liệu (Data Integration Risk):**
> PO yêu cầu dữ liệu phải được đồng bộ liên tục về Nền tảng Dữ liệu trung tâm để tính toán SAIDI/SAIFI. 
> **Câu hỏi cho PO & Data Team:** Độ trễ (latency) tối đa cho phép từ khi phiếu cập nhật trạng thái đến khi dữ liệu có mặt trên Kafka là bao nhiêu? (Đề xuất: Sử dụng Debezium CDC để đảm bảo độ trễ dưới 10 giây)[cite: 6].

> [!IMPORTANT]
> **Quy tắc Chuyển trạng thái (State Transition):**
> **Xác nhận với PO:** Vòng đời phiếu sự cố có cho phép chuyển ngược trạng thái (ví dụ từ `Done` quay lại `InProgress` nếu phát hiện lỗi chưa khắc phục triệt để) hay bắt buộc tuân thủ luồng một chiều (One-way)[cite: 4]?

> [!NOTE]
> **Phân quyền Truy cập (RBAC):**
> Thợ hiện trường và Điều độ viên trung tâm có quyền hạn khác nhau. 
> **Kiến nghị:** Thợ hiện trường chỉ có quyền tạo (`POST`) và cập nhật trạng thái, trong khi Điều độ viên có quyền xem toàn bộ danh sách (`GET`) và xóa/hủy phiếu.

---

## 3. Phân rã Kiến trúc 3 Tầng (Architectural Decomposition)

Phân rã hệ thống thành 3 lớp để định hướng quá trình sinh mã cho AI Copilot[cite: 7].

| Tầng (Layer) | Bóc tách Chi tiết (Decomposition details) |
| :--- | :--- |
| **UI Layer** | - **Outage Reporting Form:** Form nhập liệu cho thiết bị di động (nhập mã thiết bị, chọn mức độ ưu tiên).<br>- **Dispatcher Dashboard:** Bảng theo dõi thời gian thực dành cho điều độ viên, hiển thị danh sách các sự cố đang `Open` và `InProgress`. |
| **Data Layer** | - Cơ sở dữ liệu: PostgreSQL lưu trữ bảng `outage_work_orders`[cite: 6].<br>- Change Data Capture: Cấu hình Debezium lắng nghe bảng này để phát ra (emit) các sự kiện thay đổi dữ liệu lên Apache Kafka[cite: 6]. |
| **API Layer** | - `POST /api/v1/workorders`: Tiếp nhận báo cáo sự cố mất điện mới.<br>- `PATCH /api/v1/workorders/{id}/status`: Cập nhật trạng thái vòng đời.<br>- Bắt buộc tuân thủ chuẩn lỗi RFC 7807[cite: 5]. |
