<!--
Role: Principal Database Architect & Data Engineer
Task: Database schema specification, physical data types, constraints, and Flyway migration rules for Outage Management System (OMS)
Context files: docs/domain-model.md, docs/coding-rules.md, docs/ADR-001-use-h2-database.md
Constraints: Dual-DB compatibility (H2 for Dev/Test, PostgreSQL for Prod), UUID PK, strict snake_case, zero-downtime migration principles
-->
# Database & Schema Migration Specification

Tài liệu này đóng vai trò là **nguồn sự thật duy nhất (Single Source of Truth)** cho tầng lưu trữ dữ liệu (Persistence Layer) và cơ chế dịch chuyển cấu trúc dữ liệu (Flyway Schema Migration) của microservice Outage Work Order API.

Tài liệu này cung cấp đầy đủ ngữ cảnh để GitHub Copilot tự động sinh các file thực thể JPA (`@Entity`, `@Table`, `@Column`), Spring Data Repository, và các tệp migration script SQL (`src/main/resources/db/migration/V*.sql`) hoàn toàn chính xác.

---

## 1. Naming Conventions & Data Types Mapping

Hệ thống tuân thủ chặt chẽ quy chuẩn đặt tên vật lý **`snake_case`** cho bảng và cột, đảm bảo tương thích 100% giữa PostgreSQL (Production) và H2 (In-memory Dev/Test).

### Bảng Ánh xạ Kiểu Dữ liệu (Data Types Mapping)

| Java 17 Type | PostgreSQL 15+ Type | H2 (PostgreSQL Mode) Type | Ghi chú & Ràng buộc |
|---|---|---|---|
| `java.util.UUID` | `UUID` | `UUID` | Khóa chính sinh tự động hoặc qua client/service |
| `java.lang.String` | `VARCHAR(N)` | `VARCHAR(N)` | Bắt buộc định rõ chiều dài `N`, cấm để `VARCHAR` vô hạn |
| `java.time.Instant` | `TIMESTAMPTZ` | `TIMESTAMP WITH TIME ZONE` | Lưu trữ mốc thời gian chuẩn UTC |
| `java.lang.Enum` | `VARCHAR(30)` | `VARCHAR(30)` | Lưu giá trị chuỗi (`EnumType.STRING`), kết hợp `CHECK constraint` |
| `java.lang.Boolean` | `BOOLEAN` | `BOOLEAN` | Giá trị mặc định rõ ràng (`DEFAULT FALSE`) |
| `java.lang.Long` | `BIGINT` | `BIGINT` | Dành cho sequence hoặc auto-increment counter |

### Quy ước Đặt tên Đối tượng Cơ sở Dữ liệu

| Đối tượng | Quy tắc đặt tên | Ví dụ |
|---|---|---|
| **Bảng (Table)** | Số nhiều, chữ thường, `snake_case` | `work_orders`, `work_order_audits` |
| **Cột (Column)** | Danh từ/Tính từ, chữ thường, `snake_case` | `equipment_id`, `created_at`, `resolved_at` |
| **Khóa chính (PK)** | `pk_{tên_bảng}` | `pk_work_orders` |
| **Khóa ngoại (FK)** | `fk_{bảng_nguồn}_{bảng_đích}` | `fk_work_order_audits_work_orders` |
| **Chỉ mục (Index)** | `idx_{tên_bảng}_{cột_1}_{cột_2}` | `idx_work_orders_status_created_at` |
| **Ràng buộc Check** | `chk_{tên_bảng}_{tên_cột}` | `chk_work_orders_status` |

---

## 2. Table-Driven Database Schema

### Bảng `work_orders` (Phiếu Giao việc Sự cố Lưới điện)

Bảng cốt lõi quản lý thông tin và vòng đời xử lý sự cố mất điện.

| Tên Cột | Kiểu Dữ liệu | Nullable / Default | Ràng buộc Toàn vẹn (Constraints) | Ý nghĩa Nghiệp vụ |
|---|---|---|---|---|
| `id` | `UUID` | `NOT NULL` | `CONSTRAINT pk_work_orders PRIMARY KEY` | Định danh duy nhất toàn hệ thống của Work Order |
| `equipment_id` | `VARCHAR(50)` | `NOT NULL` | Độ dài tối đa 50 ký tự | Mã thiết bị lưới điện xảy ra sự cố (Trạm biến áp, máy cắt, recloser) |
| `description` | `VARCHAR(500)` | `NOT NULL` | Độ dài từ 10 đến 500 ký tự | Mô tả chi tiết nguyên nhân, vị trí sự cố do điều độ viên nhập |
| `priority` | `VARCHAR(20)` | `NOT NULL` | `CONSTRAINT chk_work_orders_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))` | Mức độ ưu tiên xử lý sự cố |
| `status` | `VARCHAR(20)` | `NOT NULL DEFAULT 'OPEN'` | `CONSTRAINT chk_work_orders_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE'))` | Trạng thái máy trạng thái đơn hướng (State Machine) |
| `created_at` | `TIMESTAMPTZ` | `NOT NULL` | Mặc định gán thời gian tạo hiện tại | Thời điểm sự cố được ghi nhận vào hệ thống |
| `resolved_at` | `TIMESTAMPTZ` | `NULL` | Cho phép null; tự động gán khi `status = 'DONE'` | Thời điểm khắc phục xong sự cố, phục vụ tính toán SAIDI/SAIFI |

---

## 3. Chiến lược Chỉ mục (Indexing Strategy) & Khóa Ngoại

Để đảm bảo hiệu năng truy vấn cao trong điều kiện tải lớn của hệ thống điều hành lưới điện (OMS):

1. **Chỉ mục kết hợp Trạng thái và Thời gian:**
   - **Tên:** `idx_work_orders_status_created_at`
   - **Cột:** `(status, created_at DESC)`
   - **Mục đích:** Tối ưu hóa API `GET /api/v1/workorders?status={status}&page={page}` giúp Dispatcher lọc nhanh các phiếu đang mở theo thứ tự mới nhất mà không gây Full Table Scan.

2. **Chỉ mục Tìm kiếm theo Thiết bị:**
   - **Tên:** `idx_work_orders_equipment_id`
   - **Cột:** `(equipment_id)`
   - **Mục đích:** Tối ưu hóa tra cứu lịch sử sự cố theo từng thiết bị lưới điện cụ thể.

3. **Chính sách Toàn vẹn Khóa ngoại (Referential Integrity):**
   - Mặc định áp dụng `ON DELETE RESTRICT` cho mọi khóa ngoại tham chiếu đến `work_orders`.
   - **Cấm Tuyệt Đối:** Sử dụng `CASCADE DELETE` trên các bảng dữ liệu vận hành sự cố, đảm bảo tuân thủ kiểm toán (Audit Trail) và an toàn dữ liệu ngành điện.

---

## 4. Quy chuẩn Quản lý Migration với Flyway

Hệ thống sử dụng **Flyway** để quản lý phiên bản cơ sở dữ liệu. Mọi thay đổi schema đều phải thông qua migration script, cấm bật `spring.jpa.hibernate.ddl-auto=update` trên môi trường Production.

### Cấu trúc Thư mục và Quy tắc Đặt tên

- **Thư mục lưu trữ:** `src/main/resources/db/migration/`
- **Quy tắc đặt tên file (Versioned Migrations):**
  ```text
  V{Phiên_bản}__{Mô_tả_ngắn_gọn_bằng_snake_case}.sql
  ```
  - Ví dụ: `V1__init_work_orders_schema.sql`, `V2__add_assigned_technician_to_work_orders.sql`.
- **Định dạng phiên bản:** Số nguyên tăng dần (`V1`, `V2`, `V3`) hoặc Timestamp (`V20260923140000__init.sql`). Đối với dự án này, chuẩn thống nhất là số nguyên tăng dần `V1`, `V2`...

### Nguyên tắc Dịch chuyển An toàn (Zero-Downtime Migration Pattern)

1. **Chỉ Bổ sung (Additive Changes First - Expand):** Khi thêm cột mới, cột đó bắt buộc phải `NULLABLE` hoặc có `DEFAULT VALUE` hợp lệ.
2. **Quy trình Xóa/Đổi tên Cột (Contract Phase):**
   - Pha 1: Thêm cột mới song song, cập nhật mã nguồn để ghi cả hai cột.
   - Pha 2: Chạy script migration backfill dữ liệu từ cột cũ sang cột mới.
   - Pha 3: Chuyển mã nguồn đọc từ cột mới.
   - Pha 4: Xóa cột cũ ở một release riêng biệt sau khi hệ thống đã ổn định.
3. **Tính Bất biến:** Tuyệt đối không chỉnh sửa nội dung của các file migration đã được merge vào nhánh `main` và đã chạy trên môi trường dùng chung.

---

## 5. DDL Script Mẫu: `V1__init_work_orders_schema.sql`

Dưới đây là nội dung chuẩn của migration script khởi tạo ban đầu, tương thích hoàn toàn cho cả PostgreSQL và H2:

```sql
-- Flyway Migration: V1__init_work_orders_schema.sql
-- Description: Khởi tạo bảng work_orders, các ràng buộc và chỉ mục cơ bản
-- Compatibility: PostgreSQL 15+, H2 Database 2.x (PostgreSQL Mode)

CREATE TABLE IF NOT EXISTS work_orders (
    id UUID NOT NULL,
    equipment_id VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_work_orders PRIMARY KEY (id),
    CONSTRAINT chk_work_orders_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_work_orders_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'DONE'))
);

-- Chỉ mục hỗ trợ truy vấn lọc theo trạng thái và sắp xếp thời gian tạo
CREATE INDEX IF NOT EXISTS idx_work_orders_status_created_at 
    ON work_orders (status, created_at DESC);

-- Chỉ mục hỗ trợ tra cứu lịch sử sự cố theo mã thiết bị
CREATE INDEX IF NOT EXISTS idx_work_orders_equipment_id 
    ON work_orders (equipment_id);
```
