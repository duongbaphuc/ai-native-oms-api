# Changelog

All notable changes to the **Outage Work Order API (`oms-api-demo`)** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-09-25

### 🚀 Official Production Release (Phiên Bản Phát Hành Chính Thức v1.0.0)

Phiên bản phát hành chính thức đầu tiên của microservice **Outage Work Order API**, được phát triển theo phương pháp luận **AI-Native SDLC** và mô hình **Spec-Driven Development** với cam kết **100% Zero-Spec-Drift** giữa mã nguồn thực thi và tài liệu đặc tả.

---

### ✨ Added (Tính năng mới)

#### 1. Core Outage Work Order RESTful API
- **Endpoints Quản lý Phiên bản v1 (`/api/v1/workorders`):**
  - `POST /api/v1/workorders`: Tiếp nhận và khởi tạo phiếu công tác mới (HTTP 201 Created + Header `Location`).
  - `GET /api/v1/workorders`: Truy vấn danh sách phiếu công tác có hỗ trợ phân trang chuẩn `PagedResponse<T>` (mặc định page 0, size 10) và lọc theo trạng thái (`OPEN`, `IN_PROGRESS`, `DONE`).
  - `GET /api/v1/workorders/{id}`: Tra cứu chi tiết phiếu công tác theo UUID (HTTP 200 OK hoặc HTTP 404 Problem Details).
  - `PATCH /api/v1/workorders/{id}/status`: Cập nhật trạng thái phiếu công tác theo máy trạng thái nghiêm ngặt (HTTP 200 OK hoặc HTTP 422 Unprocessable Content).
- **Chuẩn hóa lỗi RFC 7807 (Problem Details):** 100% phản hồi lỗi đều tuân thủ `application/problem+json` với 7 định danh URN chuẩn (`urn:problem-type:validation-error`, `resource-not-found`, `invalid-state-transition`, `bad-request`, `access-denied`, `rate-limit-exceeded`, `internal-server-error`).

#### 2. Domain-Driven Design & State Machine
- **Aggregate Root `WorkOrder`:** Đóng gói toàn vẹn logic nghiệp vụ, bảo vệ bất biến (Invariants) trước khi lưu trữ CSDL.
- **Strict State Machine:** Chuyển đổi trạng thái một chiều: `OPEN` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`. Mọi chuyển đổi nhảy cóc hoặc đảo ngược đều bị chặn với HTTP 422.
- **Priority Domain Invariant:** 4 cấp độ ưu tiên sự cố lưới điện (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).

#### 3. Security & Perimeter Defense (Bảo Mật Nhiều Lớp)
- **Dual `SecurityFilterChain`:**
  - Chuỗi `@Order(1)`: Cô lập H2 Web Console tại `@Profile("!prod")`. Trên môi trường `prod`, console bị vô hiệu hóa hoàn toàn.
  - Chuỗi `@Order(2)`: Bảo vệ toàn bộ API endpoints. Tự động chuyển đổi giữa HTTP Basic Auth (phục vụ phát triển cục bộ và kiểm thử non-prod) và OAuth2 JWT Resource Server với `JwtRoleConverter` trên môi trường Production.
- **Phân quyền dựa trên vai trò (Role-Based Access Control - RBAC):**
  - `ADMIN`: Toàn quyền thao tác trên hệ thống.
  - `DISPATCHER`: Quyền khởi tạo phiếu (`POST`) và tra cứu (`GET`).
  - `TECHNICIAN`: Quyền tra cứu (`GET`) và cập nhật trạng thái thi công (`PATCH /status`).

#### 4. Observability & Traffic Control (Giám Sát & Điều Phối)
- **Distributed Tracing (SEC-03):** `CorrelationIdFilter` tự động trích xuất hoặc khởi tạo UUID cho `X-Correlation-Id`, gán vào SLF4J/Logback MDC (`traceId`, `correlationId`) và giải phóng bộ nhớ `MDC.clear()` trong khối `finally`.
- **Rate Limiting Filter (SEC-06):** Bộ lọc giới hạn lưu lượng Token Bucket dựa trên Bucket4j theo IP máy khách (Hạn mức: 20 write req/min, 60 read req/min; cơ chế tự động dọn dẹp chống rò rỉ bộ nhớ LRU tại ngưỡng 10,000 entries).
- **Metrics & Health Probes (OPS-01):** Tích hợp Spring Boot Actuator, Prometheus Micrometer endpoint (`/actuator/prometheus`), Liveness/Readiness probes (`/actuator/health/liveness`, `/actuator/health/readiness`), và business counter metrics `oms.workorder.created.total`.

#### 5. Database & Persistence Architecture
- **Dual-Database Design:** H2 In-Memory (Dev/Test) tương thích PostgreSQL syntax; sẵn sàng kết nối PostgreSQL độc lập trên Production.
- **Flyway Database Migration:** Script `V1__init_work_orders_schema.sql` tự động đồng bộ hóa cấu trúc bảng, UUID primary key, check constraints, và các chỉ mục hiệu năng (`idx_work_orders_status_created_at`, `idx_work_orders_equipment_id`).
- **Data Integrity:** Cấu hình `spring.jpa.hibernate.ddl-auto: validate` đảm bảo schema cơ sở dữ liệu không bị tự ý can thiệp.

#### 6. Interactive Web Test Console & Developer Experience
- **Interactive Web Console (`http://localhost:8080/`):** Bảng điều khiển giao diện web trực quan với 1-click Role Switcher (`Admin`, `Dispatcher`, `Technician`, `Anonymous`), form tạo phiếu kiểm thử validation lỗi RFC 7807, và bảng điều hành trạng thái thời gian thực.
- **Non-Prod Demo Accounts:** Sẵn sàng 3 tài khoản kiểm thử cục bộ (`admin/admin123`, `dispatcher/dispatcher123`, `technician/technician123`).

#### 7. DevOps & CI/CD Infrastructure
- **Multi-stage Dockerfile:** Đóng gói container an toàn với người dùng non-root (`UID 10001:appuser`), dung lượng tối ưu trên nền Eclipse Temurin 17 JRE Alpine.
- **Docker Compose:** Cấu hình trọn gói `docker-compose.yml` tích hợp ứng dụng API và cụm CSDL PostgreSQL 16.
- **GitHub Actions CI/CD Pipeline:** Tự động hóa build Maven, kiểm thử 117 tests, thẩm định JaCoCo Quality Gate, và kiểm tra Spec-Drift-Audit gate trên mọi Pull Request.

---

### 🛡️ Quality & Test Assurance (Chất lượng & Kiểm thử)
- **117/117 Automated Test Cases:** Vượt qua 100% bộ kiểm thử tự động đa tầng:
  - Unit tests cho Domain Models (`WorkOrderTest`, `WorkOrderStatusTest`, `PriorityTest`).
  - Unit & Mockito tests cho Service (`WorkOrderServiceTest`).
  - Slice WebMvc tests cho Controllers & Exception Handlers (`WorkOrderControllerTest`, `GlobalExceptionHandlerTest`).
  - End-to-end Integration tests (`WorkOrderIntegrationTest`).
  - Security & RBAC boundary tests (`SecurityAndRbacBoundaryTests`).
  - Rate limiting & Correlation ID tests (`RateLimitingFilterTest`, `CorrelationIdFilterTest`).
- **JaCoCo Quality Gate:** Đạt **100% Line Coverage** và **100% Branch Coverage** trên toàn bộ 12 monitored classes nghiệp vụ cốt lõi.
- **Zero-Spec-Drift:** Hệ thống 16 tài liệu đặc tả sống trong `docs/` và 12 bản thảo kỹ thuật trong `docs/drafts/` phản ánh chính xác 100% hiện trạng mã nguồn thực tế.

---

### 📦 Release Artifacts
- **Executable JAR:** `oms-api-demo-1.0.0.jar`
- **Docker Image Tag:** `oms-api-demo:1.0.0`
- **Source Code:** [v1.0.0 Tag](https://github.com/duongbaphuc/ai-native-oms-api/tree/v1.0.0)

---

### 🔗 Related Documentation (Hồ Sơ Tài Liệu Liên Quan)
- [Bản Đồ Điều Hướng Ngữ Cảnh AI (`docs/03-CONTEXT_INDEX.md`)](docs/03-CONTEXT_INDEX.md)
- [Hồ Sơ Bàn Giao Kỹ Thuật & Vận Hành Hệ Thống (`docs/08-SYSTEM_HANDOVER.md`)](docs/08-SYSTEM_HANDOVER.md)
- [Hồ Sơ Đánh Giá An Ninh & Bàn Giao Bảo Mật (`docs/09-SECURITY_HANDOVER_REPORT.md`)](docs/09-SECURITY_HANDOVER_REPORT.md)
- [Cẩm Nang Kiến Trúc & Tiêu Chuẩn Java Enterprise (`docs/08-ORACLE_JAVA_DOCUMENTATION.md`)](docs/08-ORACLE_JAVA_DOCUMENTATION.md)
- [Đặc Tả Đóng Gói Container & Pipeline CI/CD (`docs/10-devops-pipeline-spec.md`)](docs/10-devops-pipeline-spec.md)
- [Báo Cáo Kiểm Toán Đồng Bộ Đặc Tả Post-Fix v1.0.0 (`docs/audit-logs/post-fix-spec-drift-audit-report-v1.0.0-final.md`)](docs/audit-logs/post-fix-spec-drift-audit-report-v1.0.0-final.md)
