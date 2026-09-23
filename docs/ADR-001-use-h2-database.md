# ADR 001: Lựa chọn H2 Database cho Môi trường Lab

**Trạng thái:** Đã phê duyệt  
**Ngày quyết định:** 2026-09-22  

## Bối cảnh (Context)
Dự án Outage Work Order cần một cơ sở dữ liệu quan hệ để lưu trữ vòng đời sự kiện theo đặc tả của phân hệ OMS. Đối với môi trường phát triển và demo cục bộ, việc thiết lập PostgreSQL qua Docker yêu cầu thêm tài nguyên và cấu hình môi trường, làm tăng thời gian onboarding.

## Quyết định (Decision)
Sử dụng **H2 Database (In-memory mode)** cho cấu hình `application-dev.yml`. Dữ liệu sẽ tự động reset sau mỗi chu kỳ khởi động ứng dụng. Chúng ta sẽ sử dụng tính năng `ddl-auto: update` của Hibernate để tự động tạo schema.

## Hậu quả (Consequences)

**Ưu điểm:**
- Khởi động tức thì (Zero-configuration startup).
- Phù hợp với phương pháp phát triển nhanh theo lát cắt dọc (Vertical slices).

**Khuyết điểm:**
- Không hỗ trợ kiểm thử tính năng CDC (Change Data Capture) bằng Debezium trực tiếp như PostgreSQL. 

> [!NOTE]
> Môi trường Integration Testing và Production vẫn bắt buộc sử dụng PostgreSQL 16.
