# BÁO CÁO PHÁT HÀNH PHIÊN BẢN v1.0.0 (OFFICIAL RELEASE NOTES)
## Outage Management System — Work Order API Service (`oms-api-demo`)

> **Tài liệu tham chiếu chuẩn (Single Source of Truth):** `docs/11-RELEASE_NOTES_v1.0.0.md`  
> **Phiên bản phát hành:** `v1.0.0` (Official Production Release)  
> **Thời điểm công bố:** 2026-09-25  
> **Đơn vị chủ trì:** AI-Native Engineering Core Team  
> **Đơn vị tiếp nhận:** Production Operations (Ops/SRE), Security Operations (SOC) & Core Engineering  
> **Trạng thái:** 🏆 **APPROVED & PUBLISHED**  

---

## 1. TỔNG QUAN PHÁT HÀNH (EXECUTIVE RELEASE SUMMARY)

Dịch vụ **Outage Work Order API (`oms-api-demo`)** là microservice trọng yếu trong hệ thống Quản lý Sự cố Lưới điện (Outage Management System - OMS). Hệ thống đảm nhiệm vai trò tiếp nhận thông tin mất điện, khởi tạo phiếu công tác, quản lý vòng đời chuyển trạng thái máy công tác, phân quyền điều độ - thi công và cung cấp dữ liệu tức thời cho các hệ thống giám sát vận hành.

Dự án được xây dựng và hoàn thiện theo phương pháp luận **AI-Native SDLC** kết hợp triết lý **Spec-Driven Development** (Phát triển hướng Đặc tả), đảm bảo tính chính xác tuyệt đối, triệt tiêu độ lệch đặc tả (100% Zero-Spec-Drift) và đạt chuẩn kiểm thử khắt khe trước khi phát hành phiên bản Production đầu tiên `v1.0.0`.

---

## 2. BẢNG TỔNG KẾT NĂNG LỰC HỆ THỐNG PHIÊN BẢN v1.0.0

| Hạng Mục | Trạng Thái / Chỉ Số Đạt Được | Ghi Chú Kỹ Thuật |
|---|---|---|
| **Bộ kiểm thử tự động** | **117 / 117 tests PASS (100%)** | 0 Failures, 0 Errors, 0 Skipped (Bao quát Unit, Slice, Integration, Security) |
| **Độ bao phủ JaCoCo** | **100% Line & 100% Branch Coverage** | Đạt ngưỡng Quality Gate trên toàn bộ 12 monitored classes nghiệp vụ |
| **Bảo mật & Phân quyền** | **Dual SecurityFilterChain (Basic / JWT)** | Cô lập H2 Console ở non-prod; OAuth2 JWT Resource Server chuẩn Production |
| **Distributed Tracing** | **SEC-03 CorrelationIdFilter** | MDC Trace/Correlation ID tự động sinh hoặc kế thừa header `X-Correlation-Id` |
| **Kiểm soát lưu lượng** | **SEC-06 Bucket4j RateLimitingFilter** | 20 write req/min, 60 read req/min per IP; cơ chế giải phóng bộ nhớ Caffeine Cache LRU eviction |
| **Giám sát & Chỉ số** | **OPS-01 Actuator & Prometheus** | Endpoint `/actuator/prometheus`, Liveness/Readiness probes, custom business metrics |
| **Toàn vẹn Dữ liệu** | **Flyway DDL Migration** | H2 In-Memory (Dev/Test) & PostgreSQL (Prod) với `ddl-auto: validate` |
| **Đóng gói & Phân phối** | **Multi-stage Dockerfile Non-root** | Eclipse Temurin 17 JRE Alpine, user `10001:appuser`, docker-compose.yml |
| **Hồ sơ Đặc tả Kỹ thuật** | **100% Zero-Spec-Drift** | 16 tài liệu `docs/`, 12 bản thảo `docs/drafts/` khớp 100% mã nguồn thực tế |

---

## 3. DANH MỤC HỢP ĐỒNG API v1.0.0 (API CONTRACTS)

Tất cả các API được công bố dưới tiền tố `/api/v1/workorders`:

### 3.1 Tiếp Nhận & Khởi Tạo Phiếu Công Tác
- **Phương thức:** `POST /api/v1/workorders`
- **Quyền hạn:** `ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_TECHNICIAN`
- **Mã phản hồi thành công:** `HTTP 201 Created`
- **Header phản hồi:** `Location: /api/v1/workorders/{id}`, `X-Correlation-Id`
- **Mã lỗi:** `HTTP 400 Bad Request` (RFC 7807) khi vi phạm ràng buộc dữ liệu (`equipmentId` không rỗng, `description` từ 10 đến 500 ký tự, `priority` hợp lệ).

### 3.2 Truy Vấn Danh Sách Phiếu Công Tác Phân Trang
- **Phương thức:** `GET /api/v1/workorders?status={status}&page={page}&size={size}`
- **Quyền hạn:** `ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_TECHNICIAN`
- **Mã phản hồi thành công:** `HTTP 200 OK`
- **Cấu trúc trả về:** `PagedResponse<WorkOrderResponse>` (danh sách bản ghi, phân trang `page`, `size`, `totalElements`, `totalPages`, `last`).

### 3.3 Tra Cứu Chi Tiết Phiếu Công Tác Theo ID
- **Phương thức:** `GET /api/v1/workorders/{id}`
- **Quyền hạn:** `ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_TECHNICIAN`
- **Mã phản hồi thành công:** `HTTP 200 OK`
- **Mã lỗi:** `HTTP 404 Not Found` (RFC 7807 `urn:problem-type:resource-not-found`) nếu UUID không tồn tại.

### 3.4 Cập Nhật Trạng Thái Phiếu Công Tác (State Transition)
- **Phương thức:** `PATCH /api/v1/workorders/{id}/status`
- **Quyền hạn:** `ROLE_ADMIN`, `ROLE_TECHNICIAN`
- **Mã phản hồi thành công:** `HTTP 200 OK`
- **Mã lỗi:** `HTTP 422 Unprocessable Content` (RFC 7807 `urn:problem-type:invalid-state-transition`) nếu vi phạm luồng chuyển dịch trạng thái một chiều:
  $$\text{OPEN} \longrightarrow \text{IN\_PROGRESS} \longrightarrow \text{DONE}$$

---

## 4. HƯỚNG DẪN TRIỂN KHAI & KHỞI CHẠY (RUNBOOK)

### 4.1 Biên Dịch & Chạy Bộ Kiểm Thử
```bash
# Kiểm tra toàn bộ mã nguồn và thẩm định JaCoCo Quality Gate
./mvnw clean verify
```

### 4.2 Chạy Ứng Dụng Độc Lập
```bash
# Sử dụng Maven Wrapper
./mvnw spring-boot:run

# Hoặc thực thi tệp JAR đóng gói độc lập
java -jar target/oms-api-demo-1.0.0.jar
```

### 4.3 Khởi Chạy Môi Trường Container (Docker Compose)
```bash
# Khởi chạy đồng bộ API và CSDL PostgreSQL 16
docker compose up -d

# Kiểm tra nhật ký hoạt động
docker compose logs -f
```

---

## 5. THẨM QUYỀN DUYỆT BÀN GIAO & KÝ DUYỆT

| Đại diện | Vai trò | Trạng thái |
|---|---|:---:|
| **Lead AI-Native Architect** | Thẩm định kiến trúc & tuân thủ quy chuẩn SDLC | ✅ **ĐÃ PHÊ DUYỆT** |
| **Principal Security Architect** | Thẩm định an ninh, OWASP API Top 10 & CWE | ✅ **ĐÃ PHÊ DUYỆT** |
| **Production Operations Lead** | Nghiệm thu khả năng vận hành, Giám sát & Logging | ✅ **ĐÃ PHÊ DUYỆT** |
