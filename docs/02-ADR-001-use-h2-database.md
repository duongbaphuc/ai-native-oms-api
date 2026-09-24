# ADR 001: Lựa chọn H2 Database cho Môi trường Lab

**Trạng thái:** Đã phê duyệt  
**Ngày quyết định:** 2026-09-22  

## Bối cảnh (Context)
Dự án Outage Work Order cần một cơ sở dữ liệu quan hệ để lưu trữ vòng đời sự kiện theo đặc tả của phân hệ OMS. Đối với môi trường phát triển và demo cục bộ, việc thiết lập PostgreSQL qua Docker yêu cầu thêm tài nguyên và cấu hình môi trường, làm tăng thời gian onboarding.

## Quyết định (Decision)
Sử dụng **H2 Database (In-memory mode)** cho cấu hình `application-dev.yml` và môi trường Lab/Local Dev. Dữ liệu sẽ tự động reset sau mỗi chu kỳ khởi động ứng dụng.

Để đảm bảo tính nhất quán tuyệt đối với môi trường PostgreSQL Production theo [`docs/02-database-migration-spec.md`](02-database-migration-spec.md):
- H2 được kích hoạt ở chế độ tương thích PostgreSQL (`MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH`).
- Cấu trúc cơ sở dữ liệu được khởi tạo và quản lý thông qua **Flyway** migration script (`src/main/resources/db/migration/V1__init_work_orders_schema.sql`).
- Cấu hình Hibernate: `spring.jpa.hibernate.ddl-auto: validate` (cấm tuyệt đối dùng `ddl-auto: update` nhằm tránh schema drift và xung đột checksum với Flyway).

## Hậu quả (Consequences)

**Ưu điểm:**
- Khởi động tức thì (Zero-configuration startup) mà không cần cài đặt Docker/PostgreSQL cục bộ.
- Đảm bảo 100% script DDL chạy kiểm thử được trên cả H2 lẫn PostgreSQL.
- Phù hợp với phương pháp phát triển nhanh theo lát cắt dọc (Vertical slices).

**Khuyết điểm:**
- Không hỗ trợ kiểm thử tính năng CDC (Change Data Capture) bằng Debezium trực tiếp như PostgreSQL. 

> [!NOTE]
> Môi trường Integration Testing và Production vẫn bắt buộc sử dụng PostgreSQL 16 kết hợp Flyway migration.
