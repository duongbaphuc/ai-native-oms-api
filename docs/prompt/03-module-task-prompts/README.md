# Module-Level Implementation Task Prompts (Bản Thảo Kỹ Thuật Theo Từng Phần)

Thư mục này quản lý và lập chỉ mục cho các câu prompt kỹ thuật chi tiết theo từng module chuyên biệt của ứng dụng Outage Work Order API.

Các prompt này được tích hợp ngay phần đầu (header comment `<!-- Role: ... Task: ... Context: ... Constraints: ... -->`) của từng file bản thảo trong `docs/drafts/`.

---

## Danh Mục Bản Thảo & Task Prompts

| Module Nghiệp Vụ | Tệp Bản Thảo Kỹ Thuật Chứa Prompt | Vai Trò Kỹ Thuật (Role) | Nhiệm Vụ Trọng Tâm (Task) |
|---|---|---|---|
| **Mapping & Thứ Tự Triển Khai** | [`docs/drafts/draft-file-mapping.md`](../../drafts/draft-file-mapping.md) | Lead Architect | Bản đồ phụ thuộc 39 file và lộ trình hiện thực hóa tuần tự (100% Zero-Draft-Drift) |
| **Domain Entity & Enums** | [`docs/drafts/draft-workorder-domain.md`](../../drafts/draft-workorder-domain.md) | Senior Domain Engineer | Khởi tạo Aggregate Root `WorkOrder`, State Machine & Enums |
| **DTOs & Data Transfer Records** | [`docs/drafts/draft-dtos.md`](../../drafts/draft-dtos.md) | Senior Backend Engineer | Các Java 17 immutable records kèm Bean Validation và static factory |
| **Service Layer Orchestration** | [`docs/drafts/draft-workorder-service.md`](../../drafts/draft-workorder-service.md) | Senior Spring Boot Engineer | Logic điều phối nghiệp vụ, transaction và kiểm tra trạng thái |
| **Tạo Phiếu Công Tác (POST)** | [`docs/drafts/draft-workorder-create.md`](../../drafts/draft-workorder-create.md) | Senior API Engineer | Endpoint `POST /api/v1/workorders` (201 Created + Location) |
| **Tra Cứu & Phân Trang (GET)** | [`docs/drafts/draft-workorder-get.md`](../../drafts/draft-workorder-get.md) | Senior API Engineer | Endpoint `GET /api/v1/workorders` (Paged) và `GET /{id}` |
| **Cập Nhật Trạng Thái (PATCH)** | [`docs/drafts/draft-workorder-patch.md`](../../drafts/draft-workorder-patch.md) | Senior API Engineer | Endpoint `PATCH /api/v1/workorders/{id}/status` (422 State Machine) |
| **Xử Lý Ngoại Lệ Toàn Cục** | [`docs/drafts/draft-global-exception-handler.md`](../../drafts/draft-global-exception-handler.md) | Senior Framework Engineer | `@RestControllerAdvice` xử lý RFC 7807 Problem Details |
| **Cấu Hình Bảo Mật & RBAC** | [`docs/drafts/draft-security-config.md`](../../drafts/draft-security-config.md) | Lead Security Engineer | Cấu hình Dual SecurityFilterChain, HTTP Basic, OAuth2 JWT và RBAC |
| **Quan Sát Phân Tán & Rate Limiting** | [`docs/drafts/draft-observability-filters.md`](../../drafts/draft-observability-filters.md) | Senior DevOps/SRE Engineer | CorrelationIdFilter (MDC traceId) và RateLimitingFilter (Bucket4j Token Bucket) |
| **Thành Phần Dùng Chung & Tiện Ích** | [`docs/drafts/draft-shared-components.md`](../../drafts/draft-shared-components.md) | Senior Software Engineer | ProblemTypes, StringToWorkOrderStatusConverter và WorkOrderTestFixtures |
| **Kiểm Thử Tự Động Toàn Diện** | [`docs/drafts/draft-workorder-tests.md`](../../drafts/draft-workorder-tests.md) | Senior QA Automation Engineer | Kim tự tháp kiểm thử 20 test classes (117 ca kiểm thử nghiệm thu) |
| **Kiểm Toán Chất Lượng 13 Tiêu Chí** | [`docs/prompt/01-sdlc-playbook/16-comprehensive-quality-audit-13-criteria.prompt.md`](../01-sdlc-playbook/16-comprehensive-quality-audit-13-criteria.prompt.md) | Principal QA Architect | Kiểm toán mã nguồn toàn diện 13 tiêu chí chất lượng (Triangulation SSOT) |
| **Khắc Phục Sau Kiểm Toán (13/13 PASS)** | [`docs/prompt/01-sdlc-playbook/17-remediation-13-criteria-quality-audit.prompt.md`](../01-sdlc-playbook/17-remediation-13-criteria-quality-audit.prompt.md) | Principal Software Architect | Tái cấu trúc loại bỏ code thừa, Caffeine LRU eviction, Javadoc, format chuẩn |
