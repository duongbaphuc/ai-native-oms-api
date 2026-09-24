# Bản Đồ Điều Hướng Ngữ Cảnh AI (AI Context Index)
**Dự án:** Outage Management System (OMS) — Outage Work Order API  
**Kiến trúc:** 3-Tier (Controller → Service → Repository), Clean Architecture, Spec-Driven Development  
**Mục tiêu:** Cung cấp mục lục điều hướng chính xác để GitHub Copilot và các AI Agents nạp đúng ngữ cảnh theo từng tác vụ, đảm bảo nguyên tắc Zero-Hallucination.

---

## 1. Nguồn Chân Lý Nghiệp Vụ & Kỹ Thuật (Single Sources of Truth - SSOT)

| Tài liệu | Vai trò | Tác vụ AI Cần Nạp |
|---|---|---|
| [`docs/br-analysis-wo.md`](br-analysis-wo.md) | **Đặc tả Nghiệp vụ Gốc (Baseline SSOT)** | Bắt buộc đọc khi bắt đầu bất kỳ tác vụ nào liên quan đến logic, phân quyền hoặc luồng sự kiện |
| [`docs/domain-model.md`](domain-model.md) | Mô hình Thực thể, Thuộc tính, Invariants & State Machine | Sinh code Entity JPA, Enums, Logic chuyển đổi trạng thái |
| [`docs/api-spec.md`](api-spec.md) | Hợp đồng REST API, Schema Request/Response, RBAC, Edge Cases | Sinh code Controller, DTOs, cấu hình Method Security |
| [`docs/database-migration-spec.md`](database-migration-spec.md) | CSDL vật lý, Flyway Script (`V1__...sql`), Indexes | Viết SQL migration, cấu hình Flyway, Entity JPA columns |
| [`docs/security-auth-spec.md`](security-auth-spec.md) | JWT Claims, RBAC Matrix, CORS, Rate Limit, Web Security | Viết `SecurityConfig`, Token converter, Security tests |

---

## 2. Quy Chuẩn Kỹ Thuật (Standards & Guardrails)

| Tài liệu | Trọng tâm Quy chuẩn |
|---|---|
| [`docs/coding-rules.md`](coding-rules.md) | Chuẩn viết mã Java 17, Spring Boot 3.3, Constructor Injection, cấm PII log |
| [`docs/api-rules.md`](api-rules.md) | Chuẩn RESTful API, `@JsonIgnoreProperties(ignoreUnknown = false)`, RFC 7807 Error Catalog |
| [`docs/internal-coding-standards.md`](internal-coding-standards.md) | Quy chuẩn UTC Instant, cấm ModelMapper, cấu trúc phân trang `PagedResponse<T>` |
| [`docs/security-rules.md`](security-rules.md) | Nguyên tắc an ninh OWASP API, Least Privilege, Input sanitization |
| [`docs/observability-and-logging.md`](observability-and-logging.md) | Cấu trúc log ECS JSON, Correlation ID Filter, Micrometer metrics |
| [`docs/ADR-001-use-h2-database.md`](ADR-001-use-h2-database.md) | Quyết định H2 (Dev/Lab) tương thích Postgres mode kết hợp Flyway |

---

## 3. Bản Thảo Cài Đặt Chi Tiết (Implementation Drafts - Copilot Blueprints)

Các tệp trong `docs/drafts/` chứa mã giả (pseudo-code), chữ ký phương thức, bảng mapping và mã nguồn mẫu để Copilot sinh code:

- [`docs/drafts/draft-file-mapping.md`](drafts/draft-file-mapping.md): Danh mục 14 file Java bắt buộc và thứ tự triển khai (Enums $\rightarrow$ Entity $\rightarrow$ Repo $\rightarrow$ DTOs $\rightarrow$ Service $\rightarrow$ Controller $\rightarrow$ Handlers $\rightarrow$ Tests).
- [`docs/drafts/draft-workorder-domain.md`](drafts/draft-workorder-domain.md): Entity `WorkOrder`, Enums `Priority`, `WorkOrderStatus` (với `canTransitionTo()`), `WorkOrderRepository`.
- [`docs/drafts/draft-dtos.md`](drafts/draft-dtos.md): `WorkOrderRequest`, `WorkOrderStatusRequest`, `WorkOrderResponse`, `PagedResponse`.
- [`docs/drafts/draft-workorder-service.md`](drafts/draft-workorder-service.md): `WorkOrderService` (4 methods, delegation, re-throw exception).
- [`docs/drafts/draft-workorder-create.md`](drafts/draft-workorder-create.md): `POST /api/v1/workorders` (201 Created + Location).
- [`docs/drafts/draft-workorder-get.md`](drafts/draft-workorder-get.md): `GET /api/v1/workorders` (Paged) và `GET /api/v1/workorders/{id}`.
- [`docs/drafts/draft-workorder-patch.md`](drafts/draft-workorder-patch.md): `PATCH /api/v1/workorders/{id}/status` (422 invalid transition).
- [`docs/drafts/draft-global-exception-handler.md`](drafts/draft-global-exception-handler.md): `GlobalExceptionHandler` (6 RFC 7807 handlers).
- [`docs/drafts/draft-workorder-tests.md`](drafts/draft-workorder-tests.md): Ma trận 16 test cases nghiệm thu và WebMvcTest code mẫu.

---

## 4. Hướng Dẫn Tải Ngữ Cảnh Theo Tác Vụ (Modular Context Loading Recipe)

Khi phát triển từng phần, lập trình viên/Copilot chỉ cần nạp các tệp ngữ cảnh sau để tránh lãng phí context window:

| Tác Vụ Triển Khai | Files Cần Nạp Vào AI Context |
|---|---|
| **Tạo Entity & Enums** | `docs/domain-model.md` + `docs/drafts/draft-workorder-domain.md` + `docs/coding-rules.md` |
| **Viết Migration Script SQL** | `docs/database-migration-spec.md` + `docs/domain-model.md` |
| **Tạo DTOs & Validation** | `docs/api-spec.md` + `docs/drafts/draft-dtos.md` + `docs/internal-coding-standards.md` |
| **Viết Service Layer** | `docs/drafts/draft-workorder-service.md` + `docs/drafts/draft-workorder-domain.md` + `docs/drafts/draft-dtos.md` |
| **Viết Controller Endpoints** | `docs/api-spec.md` + `docs/drafts/draft-workorder-*.md` + `docs/security-auth-spec.md` |
| **Viết Exception Handler** | `docs/api-rules.md` + `docs/drafts/draft-global-exception-handler.md` |
| **Viết Unit & Controller Tests** | `docs/drafts/draft-workorder-tests.md` + `docs/api-spec.md` + `docs/security-auth-spec.md` |

---

## 5. Trung Tâm Quản Trị Prompt AI (AI Prompt Repository)

Thư mục [`docs/prompt/`](prompt/README.md) quản lý toàn bộ các câu lệnh Prompt được chuẩn hóa của dự án:
- [`docs/prompt/01-sdlc-playbook/`](prompt/01-sdlc-playbook/00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md): Chuỗi 15 prompt quy trình AI-Native SDLC toàn diện từ Khởi tạo đến Tối ưu hóa Cú pháp & Hiệu năng (Pha 00 - 14).
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

| Tên Tệp Markdown | Kích Thước (Bytes) | Ước Tính Tokens | Mục Đích Chính |
|---|---|---|---|
| [`docs/br-analysis-wo.md`](br-analysis-wo.md) | ~5,050 | ~1,250 | Phân tích bài toán nghiệp vụ sự cố lưới điện |
| [`docs/domain-model.md`](domain-model.md) | ~4,870 | ~1,200 | Thực thể WorkOrder, Invariants, State Machine |
| [`docs/api-spec.md`](api-spec.md) | ~10,670 | ~2,600 | Hợp đồng REST API, RFC 7807 Error Catalog |
| [`docs/database-migration-spec.md`](database-migration-spec.md) | ~8,730 | ~2,100 | Flyway DDL `V1__...`, Indexing, Schema constraints |
| [`docs/security-auth-spec.md`](security-auth-spec.md) | ~9,150 | ~2,250 | Dual SecurityFilterChain, RBAC matrix, Security headers |
| [`docs/coding-rules.md`](coding-rules.md) | ~2,240 | ~550 | Chuẩn Java 17 records, Constructor Injection |
| [`docs/api-rules.md`](api-rules.md) | ~3,930 | ~950 | Chuẩn RESTful, JSON schema, RFC 7807 |
| [`docs/internal-coding-standards.md`](internal-coding-standards.md) | ~8,900 | ~2,200 | Chuẩn UTC Instant, PagedResponse, mapping thủ công |
| [`docs/security-rules.md`](security-rules.md) | ~2,830 | ~700 | OWASP API Top 10, Defense-in-depth guardrails |
| [`docs/observability-and-logging.md`](observability-and-logging.md) | ~8,730 | ~2,150 | Tracing, MDC logging context, Metrics |
| [`docs/ADR-001-use-h2-database.md`](ADR-001-use-h2-database.md) | ~2,050 | ~500 | Quyết định kiến trúc cơ sở dữ liệu H2 |
| [`docs/SYSTEM_HANDOVER.md`](SYSTEM_HANDOVER.md) | ~32,000 | ~7,800 | Hồ sơ bàn giao kỹ thuật toàn diện, 78 tests, Runbook |
| [`docs/SECURITY_HANDOVER_REPORT.md`](SECURITY_HANDOVER_REPORT.md) | ~29,000 | ~7,200 | Hồ sơ bàn giao an ninh, thẩm định OWASP, SEC-01..04 |

