# AI-Native SDLC Automated Verification Checklist: Docker & CI/CD Pipeline

- **Mã tính năng / Giai đoạn:** Phase 10 - Containerization, Docker Compose & CI/CD Pipeline with AI-Native Spec-Drift-Audit Gate
- **Tài liệu tham chiếu:** `docs/10-devops-pipeline-spec.md`, `docs/prompt/01-sdlc-playbook/10-docker-and-cicd-pipeline.prompt.md`
- **Thời gian thẩm định:** 2026-09-25T08:28:45Z
- **Nhánh kiểm thử (Branch):** `feature/WO-10-docker-and-cicd-pipeline`
- **Môi trường:** Java 17.0.12 (Eclipse Temurin), Spring Boot 3.3.5, Apache Maven 3.6.3
- **Thực thi bởi:** Lead DevSecOps Architect & Cloud-Native Platform Engineer
- **Trạng thái tổng thể:**  **PASSED (100% Quality Gates Satisfied — Zero Drift Confirmed)**

---

## 1. Bảng Thẩm Định Tự Động 5 Phần (Automated Verification Matrix)

### PHẦN 1: Multi-Stage Production Dockerfile & Container Hardening
| STT | Tiêu chí kỹ thuật (Technical Criteria) | Yêu cầu chuẩn | Bằng chứng kiểm chứng thực tế | Trạng thái |
|:---:|---|---|---|:---:|
| 1.1 | **Kiến trúc Multi-Stage Build** | Phân tách builder và runner | Stage 1 `maven:3.9-eclipse-temurin-17-alpine` và Stage 2 `eclipse-temurin:17-jre-alpine` | [x] PASS |
| 1.2 | **Tối ưu hóa Docker Layer Cache** | Copy riêng `pom.xml` tải dependencies trước | `COPY pom.xml ./` kèm `mvn dependency:go-offline -B` trước khi `COPY src ./src` | [x] PASS |
| 1.3 | **Nguyên tắc Non-Root User** | Cấm chạy container dưới quyền root | Tạo `appuser:appgroup` UID/GID `10001:10001`, cấu hình `USER 10001:10001` trước `ENTRYPOINT` | [x] PASS |
| 1.4 | **Tối ưu hóa JVM Container** | Nhận diện cgroups & entropy an toàn | `JAVA_TOOL_OPTIONS` thiết lập `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC` | [x] PASS |
| 1.5 | **Tích hợp Healthcheck Probe** | Giám sát Actuator định kỳ | `HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health \|\| exit 1` | [x] PASS |

### PHẦN 2: Cấu Hình Môi Trường Chạy Tích Hợp Cục Bộ (`docker-compose.yml`)
| STT | Tiêu chí kỹ thuật (Technical Criteria) | Yêu cầu chuẩn | Bằng chứng kiểm chứng thực tế | Trạng thái |
|:---:|---|---|---|:---:|
| 2.1 | **Dịch vụ `oms-api`** | Build từ Dockerfile cục bộ, expose 8080 | Ánh xạ cổng `8080:8080`, nạp datasource kết nối tới container `postgres:5432/workorderdb` | [x] PASS |
| 2.2 | **Dịch vụ `postgres`** | Phiên bản 15 Alpine, cấu hình volume | Image `postgres:15-alpine`, volume `pgdata`, `POSTGRES_DB=workorderdb`, `POSTGRES_USER=postgres` | [x] PASS |
| 2.3 | **Secret Hygiene trong Compose** | Cảnh báo mật khẩu dev/lab | Ghi chú rõ ràng `POSTGRES_PASSWORD: postgres_dev_only` chỉ dành cho Dev/Lab cục bộ | [x] PASS |
| 2.4 | **Khởi Động Đồng Bộ Theo Healthcheck** | `depends_on` có điều kiện sức khỏe | `oms-api` phụ thuộc vào `postgres` với `condition: service_healthy` (kiểm tra qua `pg_isready`) | [x] PASS |
| 2.5 | **Mạng Nội Bộ Biệt Lập** | Khai báo bridge network | Mạng nội bộ riêng biệt `oms-network` kết nối hai containers | [x] PASS |

### PHẦN 3: Thiết Lập Đường Ống CI/CD GitHub Actions (`.github/workflows/ci.yml`)
| STT | Tiêu chí kỹ thuật (Technical Criteria) | Yêu cầu chuẩn | Bằng chứng kiểm chứng thực tế | Trạng thái |
|:---:|---|---|---|:---:|
| 3.1 | **Sự Kiện Kích Hoạt (Triggers)** | Tự động hóa trên PR và Push | Kích hoạt khi có `push` hoặc `pull_request` vào nhánh `main` | [x] PASS |
| 3.2 | **Job 1: Build, Test & JaCoCo Gate** | Chạy toàn bộ tests và đo độ phủ | Ubuntu runner, JDK 17 (Temurin), `mvn clean verify -B` (89 tests), JaCoCo Quality Gate 100% | [x] PASS |
| 3.3 | **Lưu Trữ Báo Cáo Kiểm Thử** | Artifacts cho Surefire và JaCoCo | `upload-artifact@v4` lưu trữ `target/surefire-reports/` và `target/site/jacoco/` | [x] PASS |
| 3.4 | **Job 2: Quét Lỗ Hổng Bảo Mật (Trivy)** | Quét mã nguồn và thư viện phụ thuộc | `aquasecurity/trivy-action@master`, scan-type `fs`, phát hiện và chặn lỗ hổng `CRITICAL,HIGH` | [x] PASS |
| 3.5 | **Job 3: Docker Build Dry-Run** | Thẩm định đóng gói container | `docker/setup-buildx-action@v3`, build image không lỗi với `push: false` | [x] PASS |

### PHẦN 4: Chốt Chặn Kiểm Toán Ngữ Cảnh AI-Native (Job 4: `spec-drift-audit`)
| STT | Tiêu chí kỹ thuật (Technical Criteria) | Yêu cầu chuẩn | Bằng chứng kiểm chứng thực tế | Trạng thái |
|:---:|---|---|---|:---:|
| 4.1 | **Git Diff Boundary Guard** | Chống sửa code bỏ quên tài liệu | PR có thay đổi trong `src/main/` bắt buộc phải kèm cập nhật tương ứng tại `docs/` hoặc `docs/audit-logs/` | [x] PASS |
| 4.2 | **Enum Synchronization** | Đồng bộ enum code và đặc tả | So khớp 100% các giá trị `WorkOrderStatus` (`OPEN`, `IN_PROGRESS`, `DONE`) và `Priority` (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`) với `docs/01-domain-model.md` và `docs/02-api-spec.md` | [x] PASS |
| 4.3 | **RFC 7807 Problem Types URN Audit** | Đối chiếu 1-1 danh mục lỗi hệ thống | Quét toàn bộ hằng số URN trong `ProblemTypes.java` đối chiếu với bảng catalog trong `docs/02-api-spec.md` | [x] PASS |
| 4.4 | **Checklist Sign-off Verification** | Thẩm định biên bản nghiệm thu | Kiểm tra file checklist gần nhất trong `docs/audit-logs/checklist-*.md`, cấm tồn tại `[ ] FAIL`, đạt 100% `[x] PASS` | [x] PASS |
| 4.5 | **Toàn diện SDLC Playbook (Pha 05 - Pha 15)** | Kiểm định 11 giai đoạn SDLC tự động | Script `scripts/verify-sdlc-playbook.sh` kiểm tra 100% đạt chuẩn từ Pha 05 đến Pha 15 | [x] PASS |

### PHẦN 5: Giám Sát Khả Dụng & Bảo Mật Actuator Probes
| STT | Tiêu chí kỹ thuật (Technical Criteria) | Yêu cầu chuẩn | Bằng chứng kiểm chứng thực tế | Trạng thái |
|:---:|---|---|---|:---:|
| 5.1 | **Actuator Dependency** | Khai báo starter trong `pom.xml` | `spring-boot-starter-actuator` có mặt trong dependencies | [x] PASS |
| 5.2 | **Cấu Hình Mở Endpoint Thăm Dò** | Mở `/actuator/health` & `/info` | `management.endpoints.web.exposure.include: health,info`, probes bật sẵn | [x] PASS |
| 5.3 | **Public Access Cho Probes** | Cho phép probes không cần Basic Auth | `SecurityConfig` cấu hình `.requestMatchers("/actuator/health", "/actuator/info").permitAll()` | [x] PASS |
| 5.4 | **Ẩn Endpoint Nhạy Cảm** | Che giấu env, beans, mappings | Gọi `/actuator/env` trả về HTTP 401 Unauthorized, không rò rỉ biến môi trường | [x] PASS |
| 5.5 | **Live Healthcheck Probe Response** | Phản hồi đúng schema chuẩn | Endpoint `/actuator/health` trả về `{"status":"UP","groups":["liveness","readiness"]}` | [x] PASS |

---

## 2. Bằng Chứng Thực Nghiệm Trích Xuất (Live Verification Evidence)

### 2.1. Thăm dò Liveness / Readiness Actuator Probe
```bash
$ curl.exe -s http://localhost:8080/actuator/health
{"status":"UP","groups":["liveness","readiness"]}

$ curl.exe -s http://localhost:8080/actuator/info
{}

$ curl.exe -s -o nul -w "%{http_code}" http://localhost:8080/actuator/env
401
```

### 2.2. Kiểm Toán Toàn Diện Quy Trình SDLC Playbook (Phases 05 -> 15 Run)
```bash
$ ./scripts/verify-sdlc-playbook.sh
======================================================================
🚀 STARTING AI-NATIVE SDLC PLAYBOOK VERIFICATION (PHASES 05 -> 15)
======================================================================

--- [PHASE 05] Automated Testing & JaCoCo 100% Quality Gate ---
  ✅ [PASS] JaCoCo Quality Gate configured with 100% Line & Branch threshold in pom.xml
  ✅ [PASS] Test class exists: src/test/java/com/gpc/oms/domain/WorkOrderStatusTest.java
  ✅ [PASS] Test class exists: src/test/java/com/gpc/oms/domain/PriorityTest.java
  ✅ [PASS] Test class exists: src/test/java/com/gpc/oms/domain/WorkOrderTest.java
  ✅ [PASS] Test class exists: src/test/java/com/gpc/oms/service/WorkOrderServiceTest.java
  ✅ [PASS] Test class exists: src/test/java/com/gpc/oms/controller/WorkOrderControllerTest.java
  ✅ [PASS] Test class exists: src/test/java/com/gpc/oms/WorkOrderIntegrationTest.java

--- [PHASE 06] Interactive Verification & Web Test Console ---
  ✅ [PASS] Web test console asset exists: src/main/resources/static/index.html
  ✅ [PASS] Role Switcher integrated (ADMIN, DISPATCHER, TECHNICIAN)
  ✅ [PASS] Demo security credentials isolated with @Profile("!prod")

--- [PHASE 07] Comprehensive Code Review Audit ---
  ✅ [PASS] Code-vs-Spec comprehensive audit report exists: docs/archive/audit-logs/code-vs-spec-audit-report-2026-09-24-2.md
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/domain/WorkOrder.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/domain/WorkOrderStatus.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/domain/Priority.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/domain/WorkOrderRepository.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/service/WorkOrderService.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/controller/WorkOrderController.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/config/SecurityConfig.java
  ✅ [PASS] Target architecture class verified: src/main/java/com/gpc/oms/exception/ProblemTypes.java

--- [PHASE 08] System Handover Documentation ---
  ✅ [PASS] System Handover Dossier exists: docs/08-SYSTEM_HANDOVER.md
  ✅ [PASS] System Handover contains all 9 required sections (9 sections detected)

--- [PHASE 09] Security Audit & Vulnerability Assessment ---
  ✅ [PASS] Security Handover Report exists: docs/09-SECURITY_HANDOVER_REPORT.md
  ✅ [PASS] Trivy vulnerability scanner configured in CI pipeline

--- [PHASE 10] Containerization & CI/CD Pipeline ---
  ✅ [PASS] Multi-stage Dockerfile configured (builder + hardened runner)
  ✅ [PASS] Non-root container hardening enforced (USER 10001:10001)
  ✅ [PASS] docker-compose.yml configured with oms-api and postgres:15-alpine

--- [PHASE 11] Feature Evolution & Bugfix Discipline ---
  ✅ [PASS] CONTRIBUTING.md enforces feature/WO-<issue-id> branching and PR standards

--- [PHASE 12] Post-Fix Documentation Synchronization ---
  ✅ [PASS] Post-fix spec drift audit report exists: docs/archive/audit-logs/post-fix-spec-drift-audit-report-2026-09-24.md
  ✅ [PASS] WorkOrderStatus 'OPEN' synchronized between code & docs
  ✅ [PASS] WorkOrderStatus 'IN_PROGRESS' synchronized between code & docs
  ✅ [PASS] WorkOrderStatus 'DONE' synchronized between code & docs
  ✅ [PASS] Priority 'LOW' synchronized between code & docs
  ✅ [PASS] Priority 'MEDIUM' synchronized between code & docs
  ✅ [PASS] Priority 'HIGH' synchronized between code & docs
  ✅ [PASS] Priority 'CRITICAL' synchronized between code & docs

--- [PHASE 13] Coding Rules & Design Patterns Enforcement ---
  ✅ [PASS] Zero-Lombok rule satisfied: No Lombok annotations detected in project
  ✅ [PASS] Constructor Injection rule satisfied: No @Autowired field injection
  ✅ [PASS] Immutable Java 17 Record verified: WorkOrderRequest
  ✅ [PASS] Immutable Java 17 Record verified: WorkOrderResponse
  ✅ [PASS] Immutable Java 17 Record verified: WorkOrderStatusRequest
  ✅ [PASS] Immutable Java 17 Record verified: PagedResponse
  ✅ [PASS] Invariant encapsulation verified: No public setStatus method on WorkOrder entity

--- [PHASE 14] Syntax Performance & Code Reuse Optimization ---
  ✅ [PASS] Centralized RFC 7807 Problem Type URI constants in ProblemTypes.java
  ✅ [PASS] Java 17 Enhanced Switch Expression verified in WorkOrderStatus.java
  ✅ [PASS] Test Fixture Pattern verified: WorkOrderTestFixtures.java
  ✅ [PASS] UTC Instant timestamps verified on domain entity

--- [PHASE 15] Strict Checklist & Automated Audit Generation ---
  ✅ [PASS] Physical audit checklist artifact found: docs/audit-logs/checklist-docker-cicd-2026-09-25.md
  ✅ [PASS] Checklist sign-off audit PASSED: 26 criteria 100% verified [x] PASS (Zero unpassed items)

======================================================================
🎉 ALL AI-NATIVE SDLC GATES (PHASES 05 -> 15) PASSED WITH 100% SUCCESS!
======================================================================
```

### 2.3. Báo Cáo Kiểm Thử Tự Động Toàn Hệ Thống (Maven Clean Verify)
```text
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.gpc.oms.config.ActuatorSecurityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.config.H2ConsoleDevAccessTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.controller.WorkOrderControllerTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.converter.WorkOrderStatusConverterTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.domain.PriorityTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.domain.WorkOrderStatusTest
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.domain.WorkOrderTest
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.dto.DtoTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.exception.ProblemTypesTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.integration.WorkOrderIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.repository.WorkOrderRepositoryTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.gpc.oms.service.WorkOrderServiceTest
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 89, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] --- jacoco:0.8.12:check (check) @ oms-api-demo ---
[INFO] Analyzing coverage with JaCoCo 0.8.12
[INFO] All coverage checks have been met.
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 3. Kết Luận Nghiệm Thu (Architectural Sign-off)

1. **Tuân thủ Tuyệt đối Đặc tả Kỹ thuật:** Toàn bộ các yêu cầu từ `docs/10-devops-pipeline-spec.md` và `docs/prompt/01-sdlc-playbook/10-docker-and-cicd-pipeline.prompt.md` đã được hiện thực hóa 100%.
2. **Kiến trúc Container An Toàn & Tối Giản:** Image được xây dựng đa tầng, chạy bằng user không đặc quyền (`10001:10001`), hỗ trợ liveness/readiness probes, và tối ưu hóa cấp phát bộ nhớ JVM cho Container runtime.
3. **Đường Ống CI/CD Khép Kín & Tự Trị:** Bổ sung Job 4 `spec-drift-audit` ngăn chặn dứt điểm hiện tượng trôi dạt ngữ cảnh (Spec Drift), biến mọi tài liệu đặc tả thành chốt chặn kiểm thử tự động của quá trình tích hợp.
4. **Sẵn Sàng Triển Khai:** Mã nguồn đạt chất lượng bàn giao Production, sẵn sàng tạo Pull Request để hợp nhất vào nhánh `main`.
