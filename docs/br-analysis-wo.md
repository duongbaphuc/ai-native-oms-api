<!--
Role: Principal Business Analyst & Solution Architect
Task: Business Requirement Analysis & Decomposition for Outage Work Order
Context files: docs/domain-model.md, docs/api-spec.md, docs/database-migration-spec.md, docs/security-auth-spec.md
Constraints: Aligned with WorkOrder entity, work_orders table, DISPATCHER & TECHNICIAN access, RFC 7807 problem details
Target Files:
- docs/domain-model.md
- docs/api-spec.md
- src/main/java/com/gpc/oms/domain/WorkOrder.java
-->
# Phân tích Yêu cầu Nghiệp vụ (BR Analysis) - Outage Work Order

Dịch vụ Outage Work Order là một microservice cốt lõi thuộc phân hệ Outage Management System (OMS). API này cung cấp các giao thức RESTful để tạo, quản lý và theo dõi vòng đời của các sự kiện mất điện trên lưới điện. Tài liệu này bóc tách yêu cầu thô từ Product Owner (PO) thành các đặc tả kỹ thuật có thể thực thi.

---

## Yêu cầu Thô Ban Đầu (Raw Business Requirement)

> "Bộ phận Vận hành Lưới điện cần một tính năng mới trên ứng dụng để điều độ viên và thợ hiện trường báo cáo các sự kiện mất điện (Outage). Người dùng cần nhập mã thiết bị (`equipmentId`) bị lỗi, mô tả sự cố và đánh giá mức độ nghiêm trọng (`priority`). Hệ thống phải lưu lại toàn bộ vòng đời xử lý sự cố (từ lúc tạo đến lúc đóng phiếu). Dữ liệu này phải được đồng bộ liên tục về Nền tảng Dữ liệu trung tâm để phục vụ tính toán các chỉ số tin cậy cung cấp điện như SAIDI, SAIFI."

---

## 1. Danh Sách Thực Thể & Thuộc Tính (Table-Driven Entity)

- **Thực thể Cốt lõi:** `WorkOrder`
- **Tên bảng CSDL:** `work_orders`

| Thuộc tính (Java/JSON) | Kiểu Dữ liệu | Bắt buộc | Ràng buộc Nghiệp vụ | Mô tả Chi tiết |
|---|---|---|---|---|
| `id` | `UUID` | Có | Tự sinh ngẫu nhiên (UUID v4) | Mã định danh duy nhất của phiếu sự cố |
| `equipmentId` | `String` | Có | Max 50 ký tự, `@NotBlank` | Mã thiết bị lưới điện xảy ra sự cố (TBA, Recloser, Máy cắt) |
| `description` | `String` | Có | 10 - 500 ký tự, `@NotBlank` | Mô tả chi tiết hiện trường sự cố mất điện |
| `priority` | `Priority` | Có | Enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` | Mức độ nghiêm trọng của sự cố |
| `status` | `WorkOrderStatus` | Có | Enum: `OPEN`, `IN_PROGRESS`, `DONE` | Trạng thái vòng đời xử lý sự cố |
| `createdAt` | `Instant` | Có | Chuẩn UTC ISO-8601 | Thời điểm tiếp nhận sự cố vào hệ thống |
| `resolvedAt` | `Instant` | Không | Gán khi `status = DONE` | Thời điểm khắc phục xong, dùng tính SAIDI/SAIFI |

---

## 2. Quy Tắc Nghiệp Vụ & Phân Quyền (RBAC Alignment)

### 1. Phân Quyền Truy Cập
- **Điều độ viên (`ROLE_DISPATCHER`):** Tiếp nhận thông tin sự cố, tạo mới phiếu (`POST /api/v1/workorders`), tra cứu toàn bộ danh sách (`GET /api/v1/workorders`).
- **Kỹ thuật viên Hiện trường (`ROLE_TECHNICIAN`):** Báo cáo sự cố tại chỗ (`POST /api/v1/workorders`), xem danh sách và chi tiết phiếu, tiếp nhận và cập nhật tiến độ khắc phục (`PATCH /api/v1/workorders/{id}/status`).
- **Quản trị viên (`ROLE_ADMIN`):** Toàn quyền quản trị và giám sát hệ thống.

### 2. Máy Trạng Thái Đơn Hướng (One-way State Machine)
- Tuyến tính nghiêm ngặt: `OPEN` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`.
- Cấm lùi trạng thái hoặc nhảy cóc từ `OPEN` sang `DONE`.

---

## 3. Phân Rã Kiến Trúc 3 Tầng (Architectural Decomposition)

| Tầng (Layer) | Thành phần & Trách nhiệm Bóc tách |
|---|---|
| **UI Layer** | - **Dispatcher Web Portal:** Màn hình tạo phiếu sự cố và theo dõi danh sách theo thời gian thực.<br>- **Technician Mobile App:** Ứng dụng hiện trường tạo báo cáo sự cố và chuyển trạng thái sang `IN_PROGRESS` hoặc `DONE`. |
| **API Layer** | - `POST /api/v1/workorders`: Tiếp nhận báo cáo sự cố (Dành cho `DISPATCHER`, `TECHNICIAN`).<br>- `GET /api/v1/workorders`: Lấy danh sách phiếu sự cố có phân trang và lọc.<br>- `GET /api/v1/workorders/{id}`: Xem chi tiết phiếu.<br>- `PATCH /api/v1/workorders/{id}/status`: Cập nhật trạng thái (Dành cho `TECHNICIAN`, `DISPATCHER`).<br>- Chuẩn lỗi bắt buộc: RFC 7807 `application/problem+json`. |
| **Data Layer** | - Cơ sở dữ liệu: PostgreSQL (Production) / H2 (Dev/Test), bảng `work_orders`.<br>- Tích hợp CDC: Debezium CDC lắng nghe bảng `work_orders` để phát sự kiện sang Apache Kafka phục vụ tính toán SAIDI/SAIFI. |
