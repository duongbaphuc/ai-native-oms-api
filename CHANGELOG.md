# Changelog

All notable changes to the **Outage Work Order API (`oms-api-demo`)** project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-09-25

### 🚀 Official Production Release (Phiên Bản Phát Hành Chính Thức v1.0.0)

Phiên bản phát hành chính thức đầu tiên của microservice **Outage Work Order API (`oms-api-demo`)**, được phát triển theo phương pháp luận **AI-Native SDLC** và mô hình **Spec-Driven Development** với cam kết **100% Zero-Spec-Drift** giữa mã nguồn thực thi và tài liệu đặc tả.

Dự án đạt mốc kiểm thử hoàn hảo: **117/117 automated tests PASS (100%)**, đạt ngưỡng **JaCoCo Quality Gate 100% Line & Branch Coverage** trên 12 monitored classes nghiệp vụ cốt lõi, bảo mật nhiều lớp chuẩn OWASP API Security và đóng gói container multi-stage non-root.

---

### 🔧 Fixed & Hardened (Post-Release Remediation & Governance)

- **Khắc phục toàn diện 13 tiêu chí chất lượng mã nguồn ([PR #79](https://github.com/duongbaphuc/ai-native-oms-api/pull/79) - Closes [Issue #78](https://github.com/duongbaphuc/ai-native-oms-api/issues/78)):**
  - **Bảo mật DoS (SEC-06 / CWE-770 & CWE-400):** Thay thế cơ chế `buckets.clear()` trong `RateLimitingFilter.java` bằng **Caffeine Cache** (`maximumSize=10,000`, `expireAfterAccess=10m`) với thuật toán trục xuất Window TinyLFU / LRU, triệt tiêu hoàn toàn nguy cơ un-throttling và cạn kiệt bộ nhớ DoS.
  - **Clean Architecture & Tái cấu trúc:** Loại bỏ khối `try-catch` bắt và ném lại `IllegalStateException` dư thừa tại `WorkOrderService.java:updateStatus()`.
  - **Chuẩn hóa Code Formatting & Javadoc:**
    - Loại bỏ hoàn toàn wildcard imports trong `WorkOrder.java` (`java.util.*`) và `WorkOrderController.java` (`org.springframework.web.bind.annotation.*`), thay bằng imports tường minh.
    - Bổ sung Javadoc chuẩn hóa theo tiêu chuẩn Oracle cho 100% phương thức public trong `WorkOrderController.java` và `WorkOrderService.java`.
    - Ngắt dòng toàn bộ code dài trong `src/main/java` đảm bảo 0 dòng nào vượt quá 120 ký tự.
  - **Vệ sinh không gian làm việc (Workspace Hygiene):** Xóa bỏ thư mục dư thừa không thuộc phạm vi dự án `csv-vat-calculator/`.
  - **Đồng bộ hóa bản thảo (Zero-Draft-Drift):** Đồng bộ hóa 100% các bản thảo `draft-observability-filters.md`, `draft-workorder-service.md`, `draft-workorder-domain.md`, `draft-workorder-create.md`.
- **Kiểm định đồng bộ tài liệu đặc tả sau fix code - Pha 12 ([PR #81](https://github.com/duongbaphuc/ai-native-oms-api/pull/81) - Closes [Issue #80](https://github.com/duongbaphuc/ai-native-oms-api/issues/80)):**
  - Cập nhật đồng bộ `docs/02-security-auth-spec.md` (§5 Caffeine Cache LRU).
  - Cập nhật báo cáo an ninh `docs/09-SECURITY_HANDOVER_REPORT.md` (SEC-06 mitigation).
  - Cập nhật hồ sơ bàn giao `docs/08-SYSTEM_HANDOVER.md` (156 lines, 714 instructions, 100% JaCoCo coverage).
  - Cập nhật `docs/11-RELEASE_NOTES_v1.0.0.md` (bổ sung `ROLE_TECHNICIAN` cho endpoint POST).
  - Cập nhật `README.md` badge an ninh thành `Security: Hardened & Audited`.
  - Khởi tạo báo cáo kiểm toán độc lập `docs/audit-logs/post-fix-spec-drift-audit-report-2026-09-25-13-criteria.md`.
- **Tích hợp Pha 16 & 17 vào SDLC Master Playbook ([PR #83](https://github.com/duongbaphuc/ai-native-oms-api/pull/83) - Closes [Issue #82](https://github.com/duongbaphuc/ai-native-oms-api/issues/82)):**
  - Di dời `16-comprehensive-quality-audit-13-criteria.prompt.md` và `17-remediation-13-criteria-quality-audit.prompt.md` vào `docs/prompt/01-sdlc-playbook/`.
  - Cập nhật sơ đồ Mermaid 19 giai đoạn tuyến tính (Pha 00 - 17 kèm 04B) và bảng danh mục chi tiết trong `00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md`.
  - Đồng bộ hóa liên kết trong `docs/prompt/03-module-task-prompts/README.md` và `docs/03-CONTEXT_INDEX.md` (§5).
- **Cập nhật toàn diện truy vết Issues & Pull Requests trong CHANGELOG ([PR #85](https://github.com/duongbaphuc/ai-native-oms-api/pull/85) - Closes [Issue #84](https://github.com/duongbaphuc/ai-native-oms-api/issues/84)):**
  - Lập bảng ma trận đối chiếu 100% Issues và Pull Requests từ khởi tạo đến bản phát hành chính thức v1.0.0.

---

### 📦 Release Milestone & Artifacts (Pha 11 - 12)

- **Công bố Phát hành Chính thức v1.0.0 ([PR #73](https://github.com/duongbaphuc/ai-native-oms-api/pull/73) - Closes [Issue #72](https://github.com/duongbaphuc/ai-native-oms-api/issues/72)):**
  - Đóng gói bản phát hành `oms-api-demo-1.0.0.jar`, tag `v1.0.0`, tài liệu `docs/11-RELEASE_NOTES_v1.0.0.md` và thông cáo báo chí kỹ thuật.
- **Kiểm định độ lệch đặc tả Post-Release Pha 12 ([PR #71](https://github.com/duongbaphuc/ai-native-oms-api/pull/71) - Closes [Issue #70](https://github.com/duongbaphuc/ai-native-oms-api/issues/70), [PR #75](https://github.com/duongbaphuc/ai-native-oms-api/pull/75), [PR #77](https://github.com/duongbaphuc/ai-native-oms-api/pull/77)):**
  - Đồng bộ hóa toàn bộ 16 tệp tài liệu `docs/` sau khi đóng gói release artifacts.
- **Đồng bộ hóa bản thảo kỹ thuật Pha 04B & 04C ([PR #74](https://github.com/duongbaphuc/ai-native-oms-api/pull/74), [PR #76](https://github.com/duongbaphuc/ai-native-oms-api/pull/76)):**
  - Thẩm định Draft-to-Code Parity Audit: Khắc phục 12 điểm lệch bản thảo, xác nhận 13/13 hạng mục đạt chuẩn 100% Zero-Draft-Drift.

---

### 🛡️ Security Hardening & Defense-in-Depth (SEC-01 đến SEC-06)

- **SEC-01: Bảo vệ & Cô lập H2 Web Console ([PR #36](https://github.com/duongbaphuc/ai-native-oms-api/pull/36) - Fixes [Issue #29](https://github.com/duongbaphuc/ai-native-oms-api/issues/29)):**
  - Cấu hình Dual `SecurityFilterChain`: `@Order(1)` mở công khai console tại `@Profile("!prod")`; trên `prod` khóa hoàn toàn hoặc yêu cầu quyền `ADMIN`.
- **SEC-02: Giới hạn Phân trang Phòng vệ DoS ([PR #37](https://github.com/duongbaphuc/ai-native-oms-api/pull/37) - Fixes [Issue #30](https://github.com/duongbaphuc/ai-native-oms-api/issues/30)):**
  - Giới hạn cứng `max-page-size: 100` trong `application.yml` và kiểm tra tràn trang, bảo vệ heap memory khỏi cạn kiệt tài nguyên.
- **SEC-03: Distributed Tracing & MDC Logging Context ([PR #42](https://github.com/duongbaphuc/ai-native-oms-api/pull/42), [PR #67](https://github.com/duongbaphuc/ai-native-oms-api/pull/67) - Implements [Issue #31](https://github.com/duongbaphuc/ai-native-oms-api/issues/31)):**
  - Hiện thực hóa `CorrelationIdFilter`: Tự động trích xuất hoặc sinh mới `X-Correlation-Id`, gán vào SLF4J/Logback MDC (`traceId`, `correlationId`), trả về trong HTTP Response Header và dọn dẹp bộ nhớ `MDC.clear()` trong khối `finally`.
- **SEC-04: Tích hợp Flyway Migration & Schema Validation ([PR #35](https://github.com/duongbaphuc/ai-native-oms-api/pull/35) - Fixes [Issue #32](https://github.com/duongbaphuc/ai-native-oms-api/issues/32)):**
  - Tích hợp Flyway migration `V1__init_work_orders_schema.sql`, chuyển đổi cấu hình sang `spring.jpa.hibernate.ddl-auto: validate` ngăn ngừa tự ý sửa đổi schema.
- **SEC-05: Centralized OAuth2 JWT Resource Server ([PR #38](https://github.com/duongbaphuc/ai-native-oms-api/pull/38), [PR #40](https://github.com/duongbaphuc/ai-native-oms-api/pull/40), [PR #68](https://github.com/duongbaphuc/ai-native-oms-api/pull/68) - Implements [Issue #33](https://github.com/duongbaphuc/ai-native-oms-api/issues/33)):**
  - Tích hợp Spring Security OAuth2 Resource Server xác thực JWT tập trung trên môi trường Production, chuyển đổi claims role qua `JwtRoleConverter`.
- **SEC-06: Bucket4j Rate Limiting Token Bucket ([PR #39](https://github.com/duongbaphuc/ai-native-oms-api/pull/39), [PR #41](https://github.com/duongbaphuc/ai-native-oms-api/pull/41), [PR #65](https://github.com/duongbaphuc/ai-native-oms-api/pull/65) - Implements [Issue #34](https://github.com/duongbaphuc/ai-native-oms-api/issues/34)):**
  - Hiện thực hóa `RateLimitingFilter`: Giới hạn lưu lượng 20 write req/min, 60 read req/min theo IP máy khách, trả về HTTP 429 Problem Details chuẩn RFC 7807 (`urn:problem-type:rate-limit-exceeded`).

---

### 📊 Observability, Metrics & Testing Quality Gate

- **OPS-01: Actuator & Prometheus Micrometer Metrics ([PR #62](https://github.com/duongbaphuc/ai-native-oms-api/pull/62), [PR #64](https://github.com/duongbaphuc/ai-native-oms-api/pull/64) - Implements [Issue #44](https://github.com/duongbaphuc/ai-native-oms-api/issues/44)):**
  - Tích hợp Spring Boot Actuator, endpoint `/actuator/prometheus`, Liveness/Readiness probes (`/actuator/health/liveness`, `/actuator/health/readiness`), và custom business counter `oms.workorder.created.total`.
- **SPEC-01: Chuẩn hóa Đồng bộ URN Lỗi RFC 7807 (Fixes [Issue #45](https://github.com/duongbaphuc/ai-native-oms-api/issues/45)):**
  - Thống nhất danh mục 7 URN lỗi trong `ProblemTypes.java` và `docs/02-api-spec.md` (Zero Magic Strings).
- **TST-01: Thiết lập JaCoCo Quality Gate 100% ([PR #18](https://github.com/duongbaphuc/ai-native-oms-api/pull/18) - Implements [Issue #47](https://github.com/duongbaphuc/ai-native-oms-api/issues/47)):**
  - Cấu hình chốt chặn kiểm thử trong `pom.xml`: Bắt buộc đạt `1.00` (100%) Line Coverage và Branch Coverage trên toàn bộ các lớp nghiệp vụ.
- **TST-02: Bộ Kiểm Thử Đơn Vị Tầng Domain & Service (Implements [Issue #48](https://github.com/duongbaphuc/ai-native-oms-api/issues/48)):**
  - Unit tests cho Aggregate Root `WorkOrder`, State Machine `WorkOrderStatus` (ma trận 3x3), Enum `Priority`, DTO Records và Service Layer (`WorkOrderServiceTest`).
- **TST-03: Bộ Kiểm Thử Web Slice & RFC 7807 Handlers ([PR #19](https://github.com/duongbaphuc/ai-native-oms-api/pull/19), [PR #24](https://github.com/duongbaphuc/ai-native-oms-api/pull/24) - Implements [Issue #49](https://github.com/duongbaphuc/ai-native-oms-api/issues/49)):**
  - `@WebMvcTest` kiểm thử 7 nhóm ngoại lệ chuyển đổi sang RFC 7807 Problem Details; `StringToWorkOrderStatusConverter` và `@DataJpaTest` tương tác cơ sở dữ liệu.
- **TST-04: Bộ Kiểm Thử Tích Hợp End-to-End ([PR #25](https://github.com/duongbaphuc/ai-native-oms-api/pull/25) - Implements [Issue #50](https://github.com/duongbaphuc/ai-native-oms-api/issues/50)):**
  - `@SpringBootTest` kiểm thử trọn vẹn 3 kịch bản: Vòng đời nghiệp vụ hoàn chỉnh (Happy Path), Ranh giới bảo mật & phân quyền RBAC, và Kiểm định bất biến dữ liệu.
- **Interactive Web Test Console ([PR #20](https://github.com/duongbaphuc/ai-native-oms-api/pull/20), [PR #63](https://github.com/duongbaphuc/ai-native-oms-api/pull/63)):**
  - Giao diện kiểm thử trực quan tại `http://localhost:8080/` với 1-click Role Switcher (`Admin`, `Dispatcher`, `Technician`, `Anonymous`) và Real-time Event Inspector.

---

### 🏗️ Architecture, Standards, Optimization & DevOps (Pha 10 - 15)

- **Quy chuẩn Lập trình Java Enterprise & Design Patterns ([PR #51](https://github.com/duongbaphuc/ai-native-oms-api/pull/51), [PR #52](https://github.com/duongbaphuc/ai-native-oms-api/pull/52), [PR #54](https://github.com/duongbaphuc/ai-native-oms-api/pull/54)):**
  - Áp dụng Oracle Coding Rules, cấm Lombok, dùng Pure Java 17 records, Constructor Injection, và tài liệu hóa `docs/08-ORACLE_JAVA_DOCUMENTATION.md`.
- **Tối ưu hóa Cú pháp Java 17 & JVM Performance ([PR #55](https://github.com/duongbaphuc/ai-native-oms-api/pull/55), [PR #56](https://github.com/duongbaphuc/ai-native-oms-api/pull/56)):**
  - Tối ưu hóa JVM/GC, caching mảng enum values trong Converter, áp dụng Object Mother Test Fixtures (`WorkOrderTestFixtures`) tái sử dụng mã nguồn.
- **Đóng gói Container & Pipeline CI/CD ([PR #58](https://github.com/duongbaphuc/ai-native-oms-api/pull/58), [PR #69](https://github.com/duongbaphuc/ai-native-oms-api/pull/69)):**
  - Multi-stage Dockerfile tối ưu trên nền Eclipse Temurin 17 JRE Alpine với người dùng non-root (`UID 10001:appuser`), Docker Compose tích hợp PostgreSQL 16 và GitHub Actions CI/CD tự động kiểm thử.
- **Sắp xếp Hệ thống Tài liệu theo Thứ Tự SDLC ([PR #59](https://github.com/duongbaphuc/ai-native-oms-api/pull/59), [PR #60](https://github.com/duongbaphuc/ai-native-oms-api/pull/60), [PR #61](https://github.com/duongbaphuc/ai-native-oms-api/pull/61)):**
  - Tái cấu trúc toàn bộ cây thư mục `docs/` theo 15 pha tuyến tính, nâng cấp Spring Boot lên 3.3.5.
- **Tự động Xuất Bằng Chứng Kiểm Định Checklist ([PR #66](https://github.com/duongbaphuc/ai-native-oms-api/pull/66)):**
  - Thêm prompt Pha 15 và sinh tự động các artifacts kiểm định trong `docs/audit-logs/` (`checklist-work-order-lifecycle-*.md`, `checklist-docker-cicd-*.md`).
- **Hồ Sơ Bàn Giao Kỹ Thuật & Báo Cáo An Ninh ([PR #26](https://github.com/duongbaphuc/ai-native-oms-api/pull/26), [PR #28](https://github.com/duongbaphuc/ai-native-oms-api/pull/28), [PR #43](https://github.com/duongbaphuc/ai-native-oms-api/pull/43), [PR #46](https://github.com/duongbaphuc/ai-native-oms-api/pull/46), [PR #57](https://github.com/duongbaphuc/ai-native-oms-api/pull/57)):**
  - Lập hồ sơ bàn giao toàn diện `docs/08-SYSTEM_HANDOVER.md` và báo cáo an ninh `docs/09-SECURITY_HANDOVER_REPORT.md` (đạt điểm an ninh tuyệt đối).

---

### 📚 AI-Native Foundation & Specification Engineering (Pha 00 - 03)

- **Phân Tích Nghiệp Vụ & Mô Hình Hóa Miền ([PR #1](https://github.com/duongbaphuc/ai-native-oms-api/pull/1), [Issue #23](https://github.com/duongbaphuc/ai-native-oms-api/issues/23)):**
  - Khởi tạo tài liệu phân tích nghiệp vụ sự cố lưới điện WO-10432 (`docs/01-br-analysis-wo.md`) và thiết kế thực thể miền, State Machine (`docs/01-domain-model.md`).
- **Khởi Tạo Khung Quản Trị & AI Governance ([PR #2](https://github.com/duongbaphuc/ai-native-oms-api/pull/2), [PR #3](https://github.com/duongbaphuc/ai-native-oms-api/pull/3), [PR #27](https://github.com/duongbaphuc/ai-native-oms-api/pull/27)):**
  - Thiết lập `.github/copilot-instructions.md`, mẫu PR v2, bug templates, bộ quy tắc `docs/00-coding-rules.md`, `docs/00-api-rules.md`, `docs/00-security-rules.md`.
- **Hệ Thống Bản Thảo Kỹ Thuật (Blueprints) & Prompt Tái Sử Dụng ([PR #4](https://github.com/duongbaphuc/ai-native-oms-api/pull/4), [PR #5](https://github.com/duongbaphuc/ai-native-oms-api/pull/5), [PR #6](https://github.com/duongbaphuc/ai-native-oms-api/pull/6), [PR #7](https://github.com/duongbaphuc/ai-native-oms-api/pull/7), [PR #8](https://github.com/duongbaphuc/ai-native-oms-api/pull/8), [PR #9](https://github.com/duongbaphuc/ai-native-oms-api/pull/9), [PR #10](https://github.com/duongbaphuc/ai-native-oms-api/pull/10), [PR #14](https://github.com/duongbaphuc/ai-native-oms-api/pull/14)):**
  - Thiết lập 12 bản thảo kỹ thuật trong `docs/drafts/` phục vụ Copilot sinh code không ảo giác.
- **Kiểm Toán Ngữ Cảnh AI & Đồng Bộ Đặc Tả ([PR #11](https://github.com/duongbaphuc/ai-native-oms-api/pull/11), [PR #12](https://github.com/duongbaphuc/ai-native-oms-api/pull/12), [PR #13](https://github.com/duongbaphuc/ai-native-oms-api/pull/13), [PR #16](https://github.com/duongbaphuc/ai-native-oms-api/pull/16), [Issue #21](https://github.com/duongbaphuc/ai-native-oms-api/issues/21), [PR #22](https://github.com/duongbaphuc/ai-native-oms-api/pull/22), [PR #53](https://github.com/duongbaphuc/ai-native-oms-api/pull/53)):**
  - Rà soát khoảng trống logic, điều hòa các tệp đặc tả đạt mức sẵn sàng AI 99%, tạo `docs/03-CONTEXT_INDEX.md`.

---

## 🔗 Bảng Đối Chiếu Toàn Diện Issues & Pull Requests (Full Traceability Matrix)

| STT | Issue / PR | Tiêu Đề & Nội Dung Nghiệp Vụ | Phân Loại | Giai Đoạn SDLC | Trạng Thái |
|:---:|---|---|:---:|:---:|:---:|
| 1 | [PR #1](https://github.com/duongbaphuc/ai-native-oms-api/pull/1) | docs(spec): Phân tích Yêu cầu Nghiệp vụ Outage Work Order (WO-10432) | Docs | Pha 01 | Merged |
| 2 | [PR #2](https://github.com/duongbaphuc/ai-native-oms-api/pull/2) | docs(governance): gapfill bug template, copilot-instructions, PR v2, rules pack | Governance | Pha 00 | Merged |
| 3 | [PR #3](https://github.com/duongbaphuc/ai-native-oms-api/pull/3) | docs(copilot): standardize markdown documentation and AI context engineering specs | Docs | Pha 00 | Merged |
| 4 | [PR #4](https://github.com/duongbaphuc/ai-native-oms-api/pull/4) | feat(copilot): add reusable prompt files for endpoint implementation | Prompts | Pha 00 | Merged |
| 5 | [PR #5](https://github.com/duongbaphuc/ai-native-oms-api/pull/5) | Revert "feat(copilot): add reusable prompt files for endpoint implementation ." | Refactor | Pha 00 | Merged |
| 6 | [PR #6](https://github.com/duongbaphuc/ai-native-oms-api/pull/6) | feat(copilot): add reusable prompt files for implementation and review | Prompts | Pha 00 | Merged |
| 7 | [PR #7](https://github.com/duongbaphuc/ai-native-oms-api/pull/7) | docs(draft): test case WorkOrder API matrix + MockMvc sketch | Drafts | Pha 04 | Merged |
| 8 | [PR #8](https://github.com/duongbaphuc/ai-native-oms-api/pull/8) | docs: update draft global exception handler with 403, 400 and 500 handlers | Drafts | Pha 04 | Merged |
| 9 | [PR #9](https://github.com/duongbaphuc/ai-native-oms-api/pull/9) | docs(draft): add DTO draft spec for Work Order API endpoints | Drafts | Pha 04 | Merged |
| 10 | [PR #10](https://github.com/duongbaphuc/ai-native-oms-api/pull/10) | docs: fix comment syntax in draft-global-exception-handler.md | Drafts | Pha 04 | Merged |
| 11 | [PR #11](https://github.com/duongbaphuc/ai-native-oms-api/pull/11) | docs(audit): refactor draft specs for AI context readiness (91/100) | Audit | Pha 03 | Merged |
| 12 | [PR #12](https://github.com/duongbaphuc/ai-native-oms-api/pull/12) | docs(sdlc): add 360-degree context specifications for full application lifecycle | Specs | Pha 03 | Merged |
| 13 | [PR #13](https://github.com/duongbaphuc/ai-native-oms-api/pull/13) | docs(context): harmonize domain, API spec, business analysis, and rules to 99% AI-readiness | Audit | Pha 03 | Merged |
| 14 | [PR #14](https://github.com/duongbaphuc/ai-native-oms-api/pull/14) | Phase 02: Reusable packaging and DTO blueprints | Drafts | Pha 02 | Merged |
| 15 | [PR #15](https://github.com/duongbaphuc/ai-native-oms-api/pull/15) | feat(WO-domain-dto): entity state-machine plus DTOs plus RFC7807 handler plus V1 migration | Feature | Pha 04 | Merged |
| 16 | [PR #16](https://github.com/duongbaphuc/ai-native-oms-api/pull/16) | docs(audit): remediate system logic gaps, synchronize pagination, and clean up redundant context | Audit | Pha 03 | Merged |
| 17 | [PR #17](https://github.com/duongbaphuc/ai-native-oms-api/pull/17) | feat(workorder): implement 3-tier OMS API with RFC 7807, state machine, and tests | Feature | Pha 04 | Merged |
| 18 | [PR #18](https://github.com/duongbaphuc/ai-native-oms-api/pull/18) | test: add comprehensive 100% JaCoCo line and branch coverage test suite | Test | Pha 05 | Merged |
| 19 | [PR #19](https://github.com/duongbaphuc/ai-native-oms-api/pull/19) | fix(WO-jacoco): map invalid status query param to 400 validation-error plus test | Fix | Pha 05 | Merged |
| 20 | [PR #20](https://github.com/duongbaphuc/ai-native-oms-api/pull/20) | feat(console): add interactive web test console and demo security config for browser testing | Feature | Pha 06 | Merged |
| 21 | [Issue #21](https://github.com/duongbaphuc/ai-native-oms-api/issues/21) | [FEATURE] WO-REVIEW-01: Review generated code đối chiếu markdown specs | Issue | Pha 07 | Closed |
| 22 | [PR #22](https://github.com/duongbaphuc/ai-native-oms-api/pull/22) | docs(WO-REVIEW-01): review generated code vs markdown specs plus findings | Audit | Pha 07 | Merged |
| 23 | [Issue #23](https://github.com/duongbaphuc/ai-native-oms-api/issues/23) | [FEATURE] WO-001: Thiết kế Hướng Đặc tả (Spec-Driven Design) cho Outage Work Order | Issue | Pha 01 | Closed |
| 24 | [PR #24](https://github.com/duongbaphuc/ai-native-oms-api/pull/24) | fix(api): map invalid status query param to 400 plus converter and tests | Fix | Pha 04 | Merged |
| 25 | [PR #25](https://github.com/duongbaphuc/ai-native-oms-api/pull/25) | fix(security): guard demo users behind non-prod profile | Security | Pha 04 | Merged |
| 26 | [PR #26](https://github.com/duongbaphuc/ai-native-oms-api/pull/26) | docs(handover): add comprehensive system handover dossier, audit report, and AI-native SDLC playbook | Docs | Pha 08 | Merged |
| 27 | [PR #27](https://github.com/duongbaphuc/ai-native-oms-api/pull/27) | docs(prompt): organize prompts into categorized hierarchy with README catalog and dev templates | Prompts | Pha 00 | Merged |
| 28 | [PR #28](https://github.com/duongbaphuc/ai-native-oms-api/pull/28) | docs(security): add Phase 9 security audit prompt and comprehensive security handover report | Security | Pha 09 | Merged |
| 29 | [Issue #29](https://github.com/duongbaphuc/ai-native-oms-api/issues/29) | [BUG] SEC-01: Chặn truy cập công khai endpoint /h2-console/** trên profile production | Security | Pha 09 | Closed |
| 30 | [Issue #30](https://github.com/duongbaphuc/ai-native-oms-api/issues/30) | [BUG] SEC-02: Giới hạn kích thước trang phân trang (max-page-size: 100) phòng chống DoS | Security | Pha 09 | Closed |
| 31 | [Issue #31](https://github.com/duongbaphuc/ai-native-oms-api/issues/31) | [FEATURE] SEC-03: Hiện thực hóa CorrelationIdFilter cho truy vết phân tán và MDC log context | Feature | Pha 09 | Closed |
| 32 | [Issue #32](https://github.com/duongbaphuc/ai-native-oms-api/issues/32) | [BUG] SEC-04: Tích hợp Flyway migration starter và đồng bộ cấu hình ddl-auto | Bugfix | Pha 09 | Closed |
| 33 | [Issue #33](https://github.com/duongbaphuc/ai-native-oms-api/issues/33) | [FEATURE] SEC-05: Tích hợp OAuth2 Resource Server xác thực JWT tập trung trên môi trường Production | Feature | Pha 09 | Closed |
| 34 | [Issue #34](https://github.com/duongbaphuc/ai-native-oms-api/issues/34) | [FEATURE] SEC-06: Triển khai bộ lọc giới hạn tần suất gọi API (Rate Limiting) với Bucket4j | Feature | Pha 09 | Closed |
| 35 | [PR #35](https://github.com/duongbaphuc/ai-native-oms-api/pull/35) | fix(SEC-04): wire Flyway migration plus validate ddl-auto | Bugfix | Pha 09 | Merged |
| 36 | [PR #36](https://github.com/duongbaphuc/ai-native-oms-api/pull/36) | fix(SEC-01): profile-gate h2-console permit dev plus ADMIN prod | Security | Pha 09 | Merged |
| 37 | [PR #37](https://github.com/duongbaphuc/ai-native-oms-api/pull/37) | fix(SEC-02): cap page size max 100 plus overflow test | Security | Pha 09 | Merged |
| 38 | [PR #38](https://github.com/duongbaphuc/ai-native-oms-api/pull/38) | docs(prompt): add SEC-05 OAuth2 JWT resource server prompt (#33) | Prompts | Pha 09 | Merged |
| 39 | [PR #39](https://github.com/duongbaphuc/ai-native-oms-api/pull/39) | docs(prompt): add SEC-06 Rate Limiting Bucket4j prompt (#34) | Prompts | Pha 09 | Merged |
| 40 | [PR #40](https://github.com/duongbaphuc/ai-native-oms-api/pull/40) | feat(prompt): add SEC-05 OAuth2 JWT Resource Server prompt specification (#33) | Prompts | Pha 09 | Merged |
| 41 | [PR #41](https://github.com/duongbaphuc/ai-native-oms-api/pull/41) | feat(prompt): add SEC-06 Rate Limiting Filter with Bucket4j prompt specification (#34) | Prompts | Pha 09 | Merged |
| 42 | [PR #42](https://github.com/duongbaphuc/ai-native-oms-api/pull/42) | feat(prompt): add SEC-03 CorrelationIdFilter tracing prompt specification (#31) | Prompts | Pha 09 | Merged |
| 43 | [PR #43](https://github.com/duongbaphuc/ai-native-oms-api/pull/43) | docs(audit): comprehensive code vs spec audit report iteration 2.0 | Audit | Pha 07 | Merged |
| 44 | [Issue #44](https://github.com/duongbaphuc/ai-native-oms-api/issues/44) | [FEATURE] OPS-01: Tích hợp Spring Boot Actuator và Prometheus Micrometer Metrics | Feature | Pha 09 | Closed |
| 45 | [Issue #45](https://github.com/duongbaphuc/ai-native-oms-api/issues/45) | [DOCS] SPEC-01: Chuẩn hóa đồng bộ RFC 7807 Error Type trong docs/api-spec.md | Docs | Pha 02 | Closed |
| 46 | [PR #46](https://github.com/duongbaphuc/ai-native-oms-api/pull/46) | docs(security): system security audit and handover report v2.0 | Security | Pha 09 | Merged |
| 47 | [Issue #47](https://github.com/duongbaphuc/ai-native-oms-api/issues/47) | [TEST] TST-01: Cấu hình JaCoCo Plugin trong pom.xml và thiết lập Quality Gate 100% | Test | Pha 05 | Closed |
| 48 | [Issue #48](https://github.com/duongbaphuc/ai-native-oms-api/issues/48) | [TEST] TST-02: Xây dựng Tầng Unit Tests toàn diện cho Domain Entity, State Machine, DTOs & Service | Test | Pha 05 | Closed |
| 49 | [Issue #49](https://github.com/duongbaphuc/ai-native-oms-api/issues/49) | [TEST] TST-03: Xây dựng Tầng Web Slice & Exception Tests thẩm định 7 Handlers RFC 7807 | Test | Pha 05 | Closed |
| 50 | [Issue #50](https://github.com/duongbaphuc/ai-native-oms-api/issues/50) | [TEST] TST-04: Xây dựng Tầng End-to-End Integration Tests cho 3 kịch bản vòng đời sự cố | Test | Pha 05 | Closed |
| 51 | [PR #51](https://github.com/duongbaphuc/ai-native-oms-api/pull/51) | docs: add Oracle-compliant Java documentation specification (ORACLE_JAVA_DOCUMENTATION.md) | Docs | Pha 08 | Merged |
| 52 | [PR #52](https://github.com/duongbaphuc/ai-native-oms-api/pull/52) | docs: add Oracle-compliant Java documentation specification (ORACLE_JAVA_DOCUMENTATION.md) | Docs | Pha 08 | Merged |
| 53 | [PR #53](https://github.com/duongbaphuc/ai-native-oms-api/pull/53) | docs(sync): synchronize living markdown specs with codebase to achieve zero spec drift | Sync | Pha 12 | Merged |
| 54 | [PR #54](https://github.com/duongbaphuc/ai-native-oms-api/pull/54) | refactor(compliance): enforce Oracle Senior Java standards and design patterns | Refactor | Pha 13 | Merged |
| 55 | [PR #55](https://github.com/duongbaphuc/ai-native-oms-api/pull/55) | feat(prompt): add Phase 14 prompt for syntax, performance, and code reuse optimization | Prompts | Pha 14 | Merged |
| 56 | [PR #56](https://github.com/duongbaphuc/ai-native-oms-api/pull/56) | refactor(optimization): modern syntax, JVM performance, and DRY code reusability | Refactor | Pha 14 | Merged |
| 57 | [PR #57](https://github.com/duongbaphuc/ai-native-oms-api/pull/57) | docs(handover): synchronize system handover dossier with 86 tests and 12 jacoco classes | Handover | Pha 08 | Merged |
| 58 | [PR #58](https://github.com/duongbaphuc/ai-native-oms-api/pull/58) | feat(devops): implement multi-stage hardened dockerfile, docker compose, and github actions ci | DevOps | Pha 10 | Merged |
| 59 | [PR #59](https://github.com/duongbaphuc/ai-native-oms-api/pull/59) | docs(sdlc): reorganize markdown documentation by AI-Native SDLC phase order | Refactor | Pha 00 | Merged |
| 60 | [PR #60](https://github.com/duongbaphuc/ai-native-oms-api/pull/60) | docs(handover): synchronize tech stack versions to Spring Boot 3.3.5 and 89 tests across specs | Handover | Pha 08 | Merged |
| 61 | [PR #61](https://github.com/duongbaphuc/ai-native-oms-api/pull/61) | docs(sync): Phase 12 post-fix spec drift audit and zero-drift documentation synchronization | Sync | Pha 12 | Merged |
| 62 | [PR #62](https://github.com/duongbaphuc/ai-native-oms-api/pull/62) | docs(prompt): bổ sung task prompt triển khai OPS-01 Actuator & Prometheus Metrics (#44) | Prompts | Pha 09 | Merged |
| 63 | [PR #63](https://github.com/duongbaphuc/ai-native-oms-api/pull/63) | feat(console): implement interactive browser testing environment and configure demo security | Feature | Pha 06 | Merged |
| 64 | [PR #64](https://github.com/duongbaphuc/ai-native-oms-api/pull/64) | feat(OPS-01): prometheus endpoint plus business metrics and tests | Feature | Pha 09 | Merged |
| 65 | [PR #65](https://github.com/duongbaphuc/ai-native-oms-api/pull/65) | feat(security): triển khai bộ lọc giới hạn tần suất gọi API (Rate Limiting) với Bucket4j (#34) | Security | Pha 09 | Merged |
| 66 | [PR #66](https://github.com/duongbaphuc/ai-native-oms-api/pull/66) | feat(sdlc): add Phase 15 prompt and generate automated verification checklist audit artifact | Audit | Pha 15 | Merged |
| 67 | [PR #67](https://github.com/duongbaphuc/ai-native-oms-api/pull/67) | feat(observability): hiện thực hóa CorrelationIdFilter cho truy vết phân tán và MDC log context (#31) | Observability | Pha 09 | Merged |
| 68 | [PR #68](https://github.com/duongbaphuc/ai-native-oms-api/pull/68) | feat(security): tích hợp OAuth2 Resource Server xác thực JWT tập trung (#33) | Security | Pha 09 | Merged |
| 69 | [PR #69](https://github.com/duongbaphuc/ai-native-oms-api/pull/69) | feat(devops): configure multi-stage Dockerfile, docker-compose, and CI/CD with spec-drift-audit gate (#10) | DevOps | Pha 10 | Merged |
| 70 | [Issue #70](https://github.com/duongbaphuc/ai-native-oms-api/issues/70) | [DOCS] SPEC-02: Đồng bộ hóa tài liệu kỹ thuật docs/08-SYSTEM_HANDOVER.md, docs/03-CONTEXT_INDEX.md và README.md | Docs | Pha 12 | Closed |
| 71 | [PR #71](https://github.com/duongbaphuc/ai-native-oms-api/pull/71) | docs(sync): Phase 12 post-fix spec drift audit & documentation synchronization (Closes #70) | Sync | Pha 12 | Merged |
| 72 | [Issue #72](https://github.com/duongbaphuc/ai-native-oms-api/issues/72) | [RELEASE] REL-01: Phát hành phiên bản chính thức v1.0.0 (Release v1.0.0 Readiness & Final Artifacts) | Release | Pha 11 | Closed |
| 73 | [PR #73](https://github.com/duongbaphuc/ai-native-oms-api/pull/73) | release: Outage Work Order API Service v1.0.0 (Closes #72) | Release | Pha 11 | Merged |
| 74 | [PR #74](https://github.com/duongbaphuc/ai-native-oms-api/pull/74) | docs(drafts): synchronize implementation blueprints for 100% zero-draft-drift (Phase 04B) | Drafts | Pha 04B | Merged |
| 75 | [PR #75](https://github.com/duongbaphuc/ai-native-oms-api/pull/75) | docs(sync): Phase 12 post-release spec drift audit and zero-drift documentation synchronization | Sync | Pha 12 | Merged |
| 76 | [PR #76](https://github.com/duongbaphuc/ai-native-oms-api/pull/76) | docs(phase-04c): Draft-to-Code Parity Audit - 12 Drifts Fixed, 100% Zero-Drift | Audit | Pha 04C | Merged |
| 77 | [PR #77](https://github.com/duongbaphuc/ai-native-oms-api/pull/77) | docs: synchronize post-fix specification audit | Sync | Pha 12 | Merged |
| 78 | [Issue #78](https://github.com/duongbaphuc/ai-native-oms-api/issues/78) | [REFACTOR] WO-78: Khắc phục toàn diện 13 tiêu chí chất lượng mã nguồn theo khuyến nghị kiểm toán | Refactor | Pha 17 | Closed |
| 79 | [PR #79](https://github.com/duongbaphuc/ai-native-oms-api/pull/79) | refactor(quality): khắc phục toàn diện 13 tiêu chí chất lượng mã nguồn (Closes #78) | Refactor | Pha 17 | Merged |
| 80 | [Issue #80](https://github.com/duongbaphuc/ai-native-oms-api/issues/80) | docs(sync): Kiểm định đồng bộ & cập nhật hệ thống tài liệu đặc tả sau khi fix code (Giai đoạn 12) | Docs | Pha 12 | Closed |
| 81 | [PR #81](https://github.com/duongbaphuc/ai-native-oms-api/pull/81) | docs(sync): Phase 12 post-fix spec drift audit & documentation synchronization (closes #80) | Sync | Pha 12 | Merged |
| 82 | [Issue #82](https://github.com/duongbaphuc/ai-native-oms-api/issues/82) | docs(sync): Đồng bộ Master SDLC Playbook và di dời Prompts 16 & 17 vào quy trình chuẩn (Giai đoạn 12) | Docs | Pha 12 | Closed |
| 83 | [PR #83](https://github.com/duongbaphuc/ai-native-oms-api/pull/83) | docs(playbook): integrate Phases 16 & 17 into SDLC Master Playbook (closes #82) | Playbook | Pha 12 | Merged |
| 84 | [Issue #84](https://github.com/duongbaphuc/ai-native-oms-api/issues/84) | docs(changelog): Cập nhật toàn diện CHANGELOG.md truy vết đầy đủ Issues và Pull Requests từ đầu đến v1.0.0 | Docs | Pha 12 | Open |

---

### 📦 Release Artifacts
- **Executable JAR:** `oms-api-demo-1.0.0.jar`
- **Docker Image Tag:** `oms-api-demo:1.0.0`
- **Source Code:** [v1.0.0 Tag](https://github.com/duongbaphuc/ai-native-oms-api/tree/v1.0.0)
- **Official GitHub Release:** [Release v1.0.0 Notes & Assets](https://github.com/duongbaphuc/ai-native-oms-api/releases/tag/v1.0.0)

---

### 🔗 Related Documentation (Hồ Sơ Tài Liệu Liên Quan)
- [Bản Đồ Điều Hướng Ngữ Cảnh AI (`docs/03-CONTEXT_INDEX.md`)](docs/03-CONTEXT_INDEX.md)
- [Hồ Sơ Bàn Giao Kỹ Thuật & Vận Hành Hệ Thống (`docs/08-SYSTEM_HANDOVER.md`)](docs/08-SYSTEM_HANDOVER.md)
- [Hồ Sơ Đánh Giá An Ninh & Bàn Giao Bảo Mật (`docs/09-SECURITY_HANDOVER_REPORT.md`)](docs/09-SECURITY_HANDOVER_REPORT.md)
- [Cẩm Nang Kiến Trúc & Tiêu Chuẩn Java Enterprise (`docs/08-ORACLE_JAVA_DOCUMENTATION.md`)](docs/08-ORACLE_JAVA_DOCUMENTATION.md)
- [Đặc Tả Đóng Gói Container & Pipeline CI/CD (`docs/10-devops-pipeline-spec.md`)](docs/10-devops-pipeline-spec.md)
- [Báo Cáo Phát Hành Chính Thức v1.0.0 (`docs/11-RELEASE_NOTES_v1.0.0.md`)](docs/11-RELEASE_NOTES_v1.0.0.md)
- [Báo Cáo Kiểm Toán Độ Lệch Đặc Tả Post-Fix 13 Tiêu Chí (`docs/audit-logs/post-fix-spec-drift-audit-report-2026-09-25-13-criteria.md`)](docs/audit-logs/post-fix-spec-drift-audit-report-2026-09-25-13-criteria.md)
