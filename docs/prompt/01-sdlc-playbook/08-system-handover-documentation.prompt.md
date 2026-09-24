# Prompt Giai Đoạn 8: Lập Hồ Sơ Bàn Giao Kỹ Thuật & Vận Hành Hệ Thống (System Handover Dossier)

```markdown
# ROLE:
Bạn là một "Principal Technical Delivery Lead", "Lead Enterprise Architect" kiêm "Head of Site Reliability Engineering (SRE)" với hơn 15 năm kinh nghiệm về bàn giao công nghệ (Technology Handover), thẩm định tính sẵn sàng vận hành (Production Readiness Review - PRR) và xây dựng tài liệu chuyển giao kỹ thuật (Runbooks & Technical Dossier) cho các hệ thống phần mềm tài chính, năng lượng và viễn thông trọng yếu. Mục tiêu tối thượng của bạn là tổng hợp toàn bộ tri thức kỹ thuật từ mã nguồn, kiểm thử, cấu hình và tài liệu đặc tả thành một Hồ Sơ Bàn Giao Hệ Thống Chuẩn Mực duy nhất, đóng vai trò "Single Source of Truth" cho đội ngũ tiếp quản.

---

# TASK:
Thực hiện rà soát toàn diện (Comprehensive Code & Architecture Review) toàn bộ kho mã nguồn dự án (`src/main/`, `src/test/`, `pom.xml`, `src/main/resources/`, `docs/`, `scripts/`) để tổng hợp và biên soạn một **Hồ Sơ Bàn Giao Kỹ Thuật & Vận Hành Hệ Thống Toàn Diện** (System Handover Dossier), lưu tại `docs/08-SYSTEM_HANDOVER.md`.

Tài liệu bàn giao bắt buộc phải bao gồm đầy đủ 9 phần cấu trúc tiêu chuẩn:
1. **Tổng Quan Dự Án & Bối Cảnh Nghiệp Vụ (Executive Summary & System Overview):**
   - Tên hệ thống, mục tiêu kinh doanh, phạm vi chức năng, phiên bản phần mềm.
   - Triết lý kiến trúc (Domain-Driven Design, Spec-Driven Development, 3-Tier Layering, Clean Architecture).
   - Bảng kê danh mục công nghệ cốt lõi và phiên bản (Java 17, Spring Boot 3.3.4, Flyway, H2 Database, JaCoCo, Maven).
2. **Bản Đồ Kiến Trúc & Cấu Trúc Mã Nguồn (Architecture & Repository Map):**
   - Sơ đồ phân tầng và luồng dữ liệu (Mermaid Diagram: Client ➔ Filter ➔ Controller ➔ Service ➔ Repository ➔ H2 Database).
   - Danh mục các gói mã nguồn (`config`, `controller`, `domain`, `dto`, `exception`, `service`) và vai trò kỹ thuật của từng tệp.
   - Các Design Pattern và Best Practices được áp dụng (Immutable Records, State Machine transitions, Invariant validations, Static Factory methods, RFC 7807 Global Exception Handling).
3. **Mô Hình Dữ Liệu & Quản Trị Cơ Sở Dữ Liệu (Data Model & Persistence Handover):**
   - Chiến lược quản trị cơ sở dữ liệu (Flyway Versioning `V1__init_work_orders_schema.sql`).
   - Sơ đồ ERD thực thể `work_orders` (Mermaid Diagram), chi tiết các trường, kiểu dữ liệu, khóa chính UUID, Check Constraints, Indexes.
   - Hướng dẫn kết nối H2 Console cục bộ (JDBC URL, Driver, User, Password, Session management).
4. **Danh Mục Hợp Đồng API & Tích Hợp (API Inventory & Integration Contracts):**
   - Bảng tổng hợp toàn bộ Endpoints (URL, Method, Vai trò RBAC, Request DTO, Response DTO, HTTP Status Codes).
   - Cơ chế xử lý lỗi chuẩn RFC 7807 Problem Details (URN Type, Title, Status, Detail, InvalidParams).
   - Chuẩn Header tích hợp (`X-Correlation-Id`, `Content-Type: application/json; application/problem+json`).
5. **Mô Hình Bảo Mật & Xác Thực (Security & Authentication Handover):**
   - Kiến trúc bảo mật Spring Security (`SecurityFilterChain`), cơ chế Stateless Session, CSRF, FrameOptions.
   - Bảng danh mục tài khoản mặc định (In-Memory Demo Accounts: `admin`, `dispatcher`, `technician`), mật khẩu và phân quyền RBAC tương ứng.
   - Ma trận phân quyền RBAC chi tiết trên từng Endpoint.
6. **Sổ Tay Biên Dịch, Khởi Chạy & Triển Khai (Build, Run & Deployment Runbook):**
   - Yêu cầu môi trường tiền đề (JDK 17+, Maven 3.8+).
   - Các lệnh thao tác dòng lệnh (CLI): Biên dịch (`mvn clean compile`), Chạy kiểm thử (`mvn test`), Kiểm định Quality Gate (`mvn clean verify`), Khởi chạy ứng dụng (`mvn spring-boot:run`).
   - Hướng dẫn truy cập và sử dụng Bảng điều khiển kiểm thử tương tác (Interactive Test Console tại `http://localhost:8080/`).
7. **Hồ Sơ Chất Lượng & Hệ Thống Kiểm Thử (Quality Assurance & Test Suite Dossier):**
   - Kim tự tháp kiểm thử tự động (Unit Tests, Slice Tests `@WebMvcTest`, `@DataJpaTest`, Integration Tests `@SpringBootTest`).
   - Số lượng ca kiểm thử (71 tests), trạng thái thực thi (0 failures, 0 errors, 0 skipped).
   - Số liệu JaCoCo Coverage: 100% Line Coverage, 100% Branch Coverage, ngưỡng kiểm soát Quality Gate trong `pom.xml`.
8. **Sổ Tay Vận Hành, Giám Sát & Xử Lý Sự Cố (Operations, Observability & Troubleshooting):**
   - Chuẩn định dạng Logging, MDC Correlation ID propagation, cấp độ log.
   - Sổ tay chẩn đoán & xử lý các lỗi thường gặp (Port collision, In-Memory DB reset, 401/403 Authentication/Authorization, 422 Invalid Transition, Validation Errors).
9. **Kế Hoạch Bàn Giao, Nâng Cấp & Ký Nhận Nghiệm Thu (Maintenance, Evolution & Handover Sign-Off):**
   - Quy trình thêm mới Endpoint / Entity / Migration theo chuẩn Spec-Driven.
   - Danh sách đầu mối bàn giao (Contact Matrix) và Bảng tiêu chí nghiệm thu bàn giao (Sign-off Checklist).

---

# CONSTRAINTS:
1. **Tuyệt Đối Trung Thực Với Mã Nguồn (Zero Hallucination / 100% Code Fidelity):** Toàn bộ thông số cấu hình, cổng port, tài khoản mật khẩu, tên endpoint, trường dữ liệu, mã lỗi, tên class phải được trích xuất chính xác 100% từ mã nguồn thực tế trong dự án. Tuyệt đối không dùng dữ liệu giả lập hoặc placeholder (`TODO`, `TBD`, `<replace-me>`).
2. **Tính Tự Chứa (Self-Contained & Production-Ready):** Tài liệu bàn giao phải đầy đủ đến mức một kỹ sư mới tiếp nhận dự án (Onboarding Engineer) hoặc đội ngũ vận hành (Ops/SRE) có thể làm chủ hệ thống, chạy, test, debug và bảo trì mà không cần hỏi lại đội ngũ phát triển trước đó.
3. **Chuẩn Mực Định Dạng:** Sử dụng GitHub Flavored Markdown, có biểu đồ Mermaid cho kiến trúc/luồng dữ liệu/state machine, bảng Markdown cho dữ liệu cấu trúc, và khối code có cú pháp rõ ràng cho các lệnh shell / cURL.
4. **Vệ Sinh Bảo Mật:** Ghi chú cảnh báo rõ ràng đối với các cấu hình In-Memory / mật khẩu demo cục bộ và hướng dẫn cách chuyển đổi sang cấu hình môi trường sản xuất (Production Environment Variables / Secret Vault).

---

# DONE WHEN:
1. File `docs/08-SYSTEM_HANDOVER.md` được tạo hoàn chỉnh, chi tiết, chuyên nghiệp với đầy đủ 9 phần tiêu chuẩn.
2. Không còn bất kỳ placeholder hay thông tin mơ hồ nào trong tài liệu bàn giao.
3. Bảng đối chiếu mã nguồn, API, DB Schema và tài khoản phân quyền khớp 100% với hiện trạng mã nguồn của repo.
4. Mọi lệnh trong Runbook đã được kiểm chứng hoạt động thành công trên môi trường cục bộ.
5. Hồ sơ bàn giao được rà soát và sẵn sàng cho các bên liên quan (Product Owner, DevOps, QA, Onboarding Devs) ký duyệt nghiệm thu.
```
