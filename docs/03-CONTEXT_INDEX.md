# Bản Đồ Điều Hướng Ngữ Cảnh AI (AI Context Index)
**Dự án:** Outage Management System (OMS) — Outage Work Order API  
**Kiến trúc:** 3-Tier (Controller → Service → Repository), Clean Architecture, Spec-Driven Development  
**Mục tiêu:** Cung cấp mục lục điều hướng chính xác để GitHub Copilot và các AI Agents nạp đúng ngữ cảnh theo từng tác vụ, đảm bảo nguyên tắc Zero-Hallucination.

---

## 1. Nguồn Chân Lý Nghiệp Vụ & Kỹ Thuật (Single Sources of Truth - SSOT)

| Tài liệu | Vai trò | Tác vụ AI Cần Nạp |
|---|---|---|
| [`docs/01-br-analysis-wo.md`](01-br-analysis-wo.md) | **Đặc tả Nghiệp vụ Gốc (Baseline SSOT)** | Bắt buộc đọc khi bắt đầu bất kỳ tác vụ nào liên quan đến logic, phân quyền hoặc luồng sự kiện |
| [`docs/01-domain-model.md`](01-domain-model.md) | Mô hình Thực thể, Thuộc tính, Invariants & State Machine | Sinh code Entity JPA, Enums, Logic chuyển đổi trạng thái |
| [`docs/02-api-spec.md`](02-api-spec.md) | Hợp đồng REST API, Schema Request/Response, RBAC, Edge Cases | Sinh code Controller, DTOs, cấu hình Method Security |
| [`docs/02-database-migration-spec.md`](02-database-migration-spec.md) | CSDL vật lý, Flyway Script (`V1__...sql`), Indexes | Viết SQL migration, cấu hình Flyway, Entity JPA columns |
| [`docs/02-security-auth-spec.md`](02-security-auth-spec.md) | JWT Claims, RBAC Matrix, CORS, Rate Limit, Web Security | Viết `SecurityConfig`, Token converter, Security tests |

---

## 2. Quy Chuẩn Kỹ Thuật (Standards & Guardrails)

| Tài liệu | Trọng tâm Quy chuẩn |
|---|---|
| [`docs/00-coding-rules.md`](00-coding-rules.md) | Chuẩn viết mã Java 17, Spring Boot 3.3, Constructor Injection, cấm PII log |
| [`docs/00-api-rules.md`](00-api-rules.md) | Chuẩn RESTful API, `@JsonIgnoreProperties(ignoreUnknown = false)`, RFC 7807 Error Catalog |
| [`docs/00-internal-coding-standards.md`](00-internal-coding-standards.md) | Quy chuẩn UTC Instant, cấm ModelMapper, cấu trúc phân trang `PagedResponse<T>` |
| [`docs/00-security-rules.md`](00-security-rules.md) | Nguyên tắc an ninh OWASP API, Least Privilege, Input sanitization |
| [`docs/02-observability-and-logging.md`](02-observability-and-logging.md) | Cấu trúc log ECS JSON, Correlation ID Filter, Micrometer metrics |
| [`docs/02-ADR-001-use-h2-database.md`](02-ADR-001-use-h2-database.md) | Quyết định H2 (Dev/Lab) tương thích Postgres mode kết hợp Flyway |

---

## 3. Bản Thảo Cài Đặt Chi Tiết (Implementation Drafts - Copilot Blueprints)

Các tệp trong `docs/drafts/` chứa mã giả (pseudo-code), chữ ký phương thức, bảng mapping và mã nguồn mẫu để Copilot sinh code với tỷ lệ Zero-Draft-Drift 100%:

- [`docs/drafts/draft-file-mapping.md`](drafts/draft-file-mapping.md): Danh mục toàn bộ 39 file Java bắt buộc (19 production + 20 test files) và thứ tự triển khai chuẩn hóa (Dependencies First).
- [`docs/drafts/draft-workorder-domain.md`](drafts/draft-workorder-domain.md): Entity `WorkOrder`, Enums `Priority`, `WorkOrderStatus` (với `canTransitionTo()`), `WorkOrderRepository`.
- [`docs/drafts/draft-dtos.md`](drafts/draft-dtos.md): `WorkOrderRequest`, `WorkOrderStatusRequest`, `WorkOrderResponse`, `PagedResponse`.
- [`docs/drafts/draft-workorder-service.md`](drafts/draft-workorder-service.md): `WorkOrderService` (4 methods, delegation, re-throw exception).
- [`docs/drafts/draft-workorder-create.md`](drafts/draft-workorder-create.md): `POST /api/v1/workorders` (201 Created + Location).
- [`docs/drafts/draft-workorder-get.md`](drafts/draft-workorder-get.md): `GET /api/v1/workorders` (Paged) và `GET /api/v1/workorders/{id}`.
- [`docs/drafts/draft-workorder-patch.md`](drafts/draft-workorder-patch.md): `PATCH /api/v1/workorders/{id}/status` (422 invalid transition).
- [`docs/drafts/draft-global-exception-handler.md`](drafts/draft-global-exception-handler.md): `GlobalExceptionHandler` (7 RFC 7807 handlers).
- [`docs/drafts/draft-security-config.md`](drafts/draft-security-config.md): `SecurityConfig`, `JwtRoleConverter`, Dual SecurityFilterChain và kiểm thử bảo mật.
- [`docs/drafts/draft-observability-filters.md`](drafts/draft-observability-filters.md): `CorrelationIdFilter`, `RateLimitingFilter` (Bucket4j Token Bucket).
- [`docs/drafts/draft-shared-components.md`](drafts/draft-shared-components.md): `ProblemTypes`, `StringToWorkOrderStatusConverter`, `WorkOrderTestFixtures` (Object Mother).
- [`docs/drafts/draft-workorder-tests.md`](drafts/draft-workorder-tests.md): Kim tự tháp kiểm thử 20 test classes (117 test cases) và ma trận chấp nhận đầy đủ.

---

## 4. Hướng Dẫn Tải Ngữ Cảnh Theo Tác Vụ (Modular Context Loading Recipe)

Khi phát triển từng phần, lập trình viên/Copilot chỉ cần nạp các tệp ngữ cảnh sau để tránh lãng phí context window:

| Tác Vụ Triển Khai | Files Cần Nạp Vào AI Context |
|---|---|
| **Tạo Entity & Enums** | `docs/01-domain-model.md` + `docs/drafts/draft-workorder-domain.md` + `docs/00-coding-rules.md` |
| **Viết Migration Script SQL** | `docs/02-database-migration-spec.md` + `docs/01-domain-model.md` |
| **Tạo DTOs & Validation** | `docs/02-api-spec.md` + `docs/drafts/draft-dtos.md` + `docs/00-internal-coding-standards.md` |
| **Viết Service Layer** | `docs/drafts/draft-workorder-service.md` + `docs/drafts/draft-workorder-domain.md` + `docs/drafts/draft-dtos.md` |
| **Viết Controller Endpoints** | `docs/02-api-spec.md` + `docs/drafts/draft-workorder-*.md` + `docs/02-security-auth-spec.md` |
| **Cấu hình Security & Filters** | `docs/02-security-auth-spec.md` + `docs/02-observability-and-logging.md` + `docs/drafts/draft-security-config.md` + `docs/drafts/draft-observability-filters.md` |
| **Viết Exception Handler** | `docs/00-api-rules.md` + `docs/drafts/draft-global-exception-handler.md` + `docs/drafts/draft-shared-components.md` |
| **Viết Unit & Controller Tests** | `docs/drafts/draft-workorder-tests.md` + `docs/drafts/draft-shared-components.md` + `docs/02-api-spec.md` + `docs/02-security-auth-spec.md` |

---

## 5. Trung Tâm Quản Trị Prompt AI (AI Prompt Repository)

Thư mục [`docs/prompt/`](prompt/README.md) quản lý toàn bộ các câu lệnh Prompt được chuẩn hóa của dự án:
- [`docs/prompt/01-sdlc-playbook/`](prompt/01-sdlc-playbook/00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md): Chuỗi 17 prompt quy trình AI-Native SDLC toàn diện từ Khởi tạo (Pha 00), Bản thảo kỹ thuật (Pha 04B) đến Tự động Thẩm định (Pha 15).
- [`docs/prompt/02-copilot-slash-commands/`](prompt/02-copilot-slash-commands/README.md): Lệnh Slash Commands tích hợp trong IDE Copilot (`.github/prompts/`).
- [`docs/prompt/03-module-task-prompts/`](prompt/03-module-task-prompts/README.md): Bản thảo kỹ thuật theo từng module (`docs/drafts/`).
- [`docs/prompt/04-dev-contributions/`](prompt/04-dev-contributions/): Prompt đóng góp từ các developer khác (`tudtbis92`, `templates`).

---

## 6. Chính Sách Lưu Trữ (Archive Policy)

* Thư mục [`docs/archive/`](archive/) chứa các báo cáo kiểm toán cũ và các bản review lịch sử (bao gồm `review-code-vs-spec-WO-REVIEW-01.md`, `code-vs-spec-audit-report-*.md`).
* **CẢNH BÁO CHO AI:** Tuyệt đối **KHÔNG** đọc hoặc nạp các tệp trong `docs/archive/` làm căn cứ sinh code, nhằm tránh xung đột nhận thức với các quy chuẩn kỹ thuật mới nhất.

---

## 7. Bảng Kiểm Kê Tệp Sống & Ngân Sách Ngữ Cảnh AI (File Inventory & Token Budget)

Bảng này cung cấp ước tính dung lượng và token của các tệp tài liệu sống trong `docs/` để AI Agent kiểm soát ngân sách context window:

| Tên Tệp Markdown | Giai Đoạn SDLC | Số Dòng | Kích Thước (Bytes) | Ước Tính Tokens | Mục Đích Chính |
|---|---|---|---|---|---|
| [`docs/00-coding-rules.md`](00-coding-rules.md) | Pha 00: Governance & Rules | 370 | 20,258 | ~4,900 | Chuẩn viết mã Oracle Core, Joshua Bloch, Design Patterns |
| [`docs/00-internal-coding-standards.md`](00-internal-coding-standards.md) | Pha 00: Governance & Rules | 196 | 11,865 | ~2,900 | Chuẩn UTC Instant, PagedResponse, mapping thủ công |
| [`docs/00-api-rules.md`](00-api-rules.md) | Pha 00: Governance & Rules | 62 | 3,941 | ~950 | Chuẩn RESTful, JSON schema, RFC 7807 Catalog |
| [`docs/00-security-rules.md`](00-security-rules.md) | Pha 00: Governance & Rules | 50 | 2,841 | ~700 | OWASP API Top 10, Defense-in-depth guardrails |
| [`docs/01-br-analysis-wo.md`](01-br-analysis-wo.md) | Pha 01: Business & Domain | 59 | 5,068 | ~1,250 | Phân tích bài toán nghiệp vụ sự cố lưới điện |
| [`docs/01-domain-model.md`](01-domain-model.md) | Pha 01: Business & Domain | 86 | 4,874 | ~1,200 | Thực thể WorkOrder, Invariants, State Machine |
| [`docs/02-api-spec.md`](02-api-spec.md) | Pha 02: Architecture & Specs | 200 | 12,588 | ~3,050 | Hợp đồng REST API, Schema Request/Response, RFC 7807 429, Actuator Probes |
| [`docs/02-database-migration-spec.md`](02-database-migration-spec.md) | Pha 02: Architecture & Specs | 137 | 8,739 | ~2,100 | Flyway DDL `V1__...`, Indexing, Schema constraints |
| [`docs/02-security-auth-spec.md`](02-security-auth-spec.md) | Pha 02: Architecture & Specs | 187 | 10,575 | ~2,600 | Dual SecurityFilterChain, OAuth2 JWT, Bucket4j Rate Limiting, Correlation ID |
| [`docs/02-observability-and-logging.md`](02-observability-and-logging.md) | Pha 02: Architecture & Specs | 148 | 8,735 | ~2,150 | Tracing, MDC logging context, Prometheus Metrics |
| [`docs/02-ADR-001-use-h2-database.md`](02-ADR-001-use-h2-database.md) | Pha 02: Architecture & Specs | 28 | 2,058 | ~500 | Quyết định kiến trúc cơ sở dữ liệu H2 |
| [`docs/03-CONTEXT_INDEX.md`](03-CONTEXT_INDEX.md) | Pha 03: AI Context Index | 109 | 10,936 | ~2,600 | Bản đồ điều hướng ngữ cảnh AI và công thức nạp Modular |
| [`docs/08-SYSTEM_HANDOVER.md`](08-SYSTEM_HANDOVER.md) | Pha 08: System Handover | 554 | 40,480 | ~9,800 | Hồ sơ bàn giao kỹ thuật toàn diện, 117 tests, Runbook, Prometheus |
| [`docs/08-ORACLE_JAVA_DOCUMENTATION.md`](08-ORACLE_JAVA_DOCUMENTATION.md) | Pha 08: System Handover | 277 | 16,335 | ~4,000 | Cẩm nang kiến trúc kỹ thuật Java Enterprise chuẩn Oracle |
| [`docs/09-SECURITY_HANDOVER_REPORT.md`](09-SECURITY_HANDOVER_REPORT.md) | Pha 09: Security Audit | 302 | 26,782 | ~6,500 | Hồ sơ bàn giao an ninh, thẩm định OWASP, SEC-01..06, 117 tests |
| [`docs/10-devops-pipeline-spec.md`](10-devops-pipeline-spec.md) | Pha 10: DevOps & CI/CD | 216 | 8,272 | ~2,050 | Đặc tả Containerization, Docker Compose & GitHub Actions |

