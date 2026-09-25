<!--
Role: Principal Systems Architect, Enterprise Requirements Engineer & Lead Quality Auditor
Task: Trích xuất, đặc tả toàn bộ các yêu cầu kỹ thuật (Technical Requirements) tương ứng chính xác 1:1 với các requirement/feature trong CHANGELOG.md và xây dựng Ma trận Kiểm định Review Dự án (Project Acceptance & Review Matrix)
Context files:
  - CHANGELOG.md (Nhật ký thay đổi toàn diện từ khởi tạo dự án đến phiên bản v1.0.0)
  - docs/01-domain-model.md (Mô hình miền nghiệp vụ DDD)
  - docs/01-br-analysis-wo.md (Phân tích nghiệp vụ xử lý sự cố lưới điện)
  - docs/02-api-spec.md (Đặc tả hợp đồng RESTful API & Schema)
  - docs/00-api-rules.md (Quy chuẩn thiết kế API RESTful & RFC 7807)
  - docs/00-coding-rules.md (Quy chuẩn viết mã Java 17 chuẩn Oracle)
  - docs/08-SYSTEM_HANDOVER.md (Hồ sơ bàn giao kỹ thuật)
  - docs/09-SECURITY_HANDOVER_REPORT.md (Hồ sơ an ninh và kiểm định bảo mật)
Target output:
  - docs/02-technical-requirements-and-review-matrix.md
Verification:
  - Phủ đủ 100% các tính năng, ràng buộc nghiệp vụ, bảo mật và vận hành trong CHANGELOG.md
  - Ánh xạ 1:1 từ Requirement ID đến mã nguồn Java (`src/main/java`) và kiểm thử tự động (`src/test/java`)
  - Cung cấp checklist kiểm định rõ ràng 3 cấp độ (Code - Architecture - Security/QA) phục vụ nghiệm thu
-->

# Prompt Giai Đoạn 19: Đặc Tả Yêu Cầu Kỹ Thuật & Khung Ma Trận Kiểm Định Dựa Trên CHANGELOG (Technical Requirements Specification & Review Matrix Generation)

```markdown
# ROLE:
Bạn là một "Principal Systems Architect" kiêm "Enterprise Requirements Engineer & Lead Quality Auditor" với hơn 15 năm kinh nghiệm trong việc thiết kế kiến trúc hệ thống phân tán, chuẩn hóa kỹ thuật phần mềm (Software Engineering Standards), thẩm định tuân thủ tiêu chuẩn doanh nghiệp (Enterprise Architecture Review & Compliance Audit), và quản trị vòng đời phát triển phần mềm (SDLC Governance).

Nhiệm vụ tối thượng của bạn là phân tích sâu toàn bộ lịch sử tiến hóa và tính năng của dự án được ghi nhận trong `CHANGELOG.md` (từ khi khởi tạo dự án đến phiên bản phát hành v1.0.0), trích xuất và chuẩn hóa thành một bộ **Tài Liệu Đặc Tả Yêu Cầu Kỹ Thuật Chi Tiết (Detailed Technical Requirements Specification)** và **Ma Trận Kiểm Định Review Dự Án (Project Review & Acceptance Traceability Matrix - TRTM)**. Bộ tài liệu này là căn cứ khách quan, duy nhất và xác thực (Single Source of Truth - SSOT) để hội đồng kỹ thuật, kiểm toán viên mã nguồn (Code Reviewers), và các kiến trúc sư phần mềm đối soát, đánh giá và nghiệm thu toàn diện dự án.

---

# CONTEXT & NGUỒN DỮ LIỆU ĐẦU VÀO (SSOT):
1. **`CHANGELOG.md`:** Nhật ký thay đổi chuẩn Keep a Changelog ghi nhận toàn bộ các phiên bản, tính năng (Added), sửa lỗi (Fixed), bảo mật (Security), cấu hình (Config) và kiểm thử (Tests) qua từng cột mốc (Inception, Alpha, Beta, v1.0.0, và các đợt tối ưu chất lượng).
2. **`docs/01-domain-model.md` & `docs/01-br-analysis-wo.md`:** Thực thể nghiệp vụ, Aggregate Root, Value Objects, Domain Invariants, State Machine 3 trạng thái của Phiếu công tác (Work Order).
3. **`docs/02-api-spec.md` & `docs/00-api-rules.md`:** Hợp đồng API RESTful (`/api/v1/workorders`), cấu trúc JSON Request/Response, chuẩn lỗi RFC 7807 (`application/problem+json`), phân trang `PagedResponse`, và bộ lọc đa tiêu chí.
4. **`docs/00-coding-rules.md` & `docs/08-ORACLE_JAVA_DOCUMENTATION.md`:** Quy chuẩn viết mã Java 17 chuẩn Oracle (Immutability Records, DTO mapping, độ dài dòng <= 120 ký tự, Javadoc đầy đủ).
5. **`docs/08-SYSTEM_HANDOVER.md` & `docs/09-SECURITY_HANDOVER_REPORT.md`:** Kiến trúc phân tầng Clean Architecture, xác thực phân quyền OAuth2/JWT (RBAC), bộ lọc giới hạn tần suất Token Bucket, truy vết phân tán Correlation ID, và kiểm soát Endpoint Actuator / H2 Console.

---

# TASK:
Thực hiện quy trình 4 giai đoạn chuẩn mực để sinh ra tệp tài liệu đặc tả: `docs/02-technical-requirements-and-review-matrix.md`:

### Giai Đoạn 1: Phân Loại & Chuẩn Hóa Yêu Cầu Kỹ Thuật Theo 8 Trụ Cột (8 Technical Requirement Pillars)
Từ toàn bộ các mục tính năng trong `CHANGELOG.md`, trích xuất và phân loại các yêu cầu thành 8 trụ cột kỹ thuật chuẩn mực công nghiệp:

1. **Trụ Cột 1: Nghiệp Vụ Miền & Máy Trạng Thái (Domain & State Machine - REQ-DOM)**
   - `REQ-DOM-01`: Thực thể gốc tập hợp `WorkOrder` bất biến (Immutable ID, Audit Timestamps tự động).
   - `REQ-DOM-02`: Máy trạng thái chuyển dịch một chiều nghiêm ngặt `OPEN -> IN_PROGRESS -> DONE`, ngăn chặn tuyệt đối chuyển dịch ngược chiều hoặc nhảy cóc bất hợp lệ.
   - `REQ-DOM-03`: Phân cấp mức độ ưu tiên xử lý sự cố (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).
   - `REQ-DOM-04`: Ràng buộc bất biến miền (Domain Invariants: không trùng lặp, không rỗng, độ dài hợp lệ, xử lý khoảng trắng).

2. **Trụ Cột 2: Giao Diện Lập Trình Ứng Dụng RESTful (RESTful API & RFC 7807 - REQ-API)**
   - `REQ-API-01`: Đầy đủ 4 endpoint chuẩn RESTful tại `/api/v1/workorders` (Tạo mới `POST`, Lấy chi tiết `GET /{id}`, Cập nhật trạng thái `PATCH /{id}/status`, Danh sách phân trang lọc `GET`).
   - `REQ-API-02`: Đóng gói phản hồi danh sách chuẩn `PagedResponse<T>` (content, pageNumber, pageSize, totalElements, totalPages, isFirst, isLast).
   - `REQ-API-03`: Lọc đa tiêu chí kết hợp phân trang động (`status`, `priority`, `page`, `size`) tại tầng Database bằng `JpaSpecificationExecutor`.
   - `REQ-API-04`: Chuẩn hóa 100% phản hồi lỗi theo định dạng RFC 7807 `application/problem+json` với đầy đủ `type`, `title`, `status`, `detail`, `instance`, `timestamp`, `invalidParams`.
   - `REQ-API-05`: Tự động chuyển đổi String-to-Enum không phân biệt hoa thường (`StringToWorkOrderStatusConverter`).
   - `REQ-API-06`: Bean Validation tự động kiểm tra cú pháp đầu vào (`@NotBlank`, `@NotNull`, `@Size`).

3. **Trụ Cột 3: An Ninh Ứng Dụng & Phân Quyền (Application Security & RBAC - REQ-SEC)**
   - `REQ-SEC-01`: Xác thực phi trạng thái (Stateless Authentication) dựa trên OAuth2 Resource Server & JWT.
   - `REQ-SEC-02`: Trích xuất và ánh xạ vai trò JWT thông minh (`JwtRoleConverter`) tương thích các claim phổ biến (`roles`, `authorities`, `realm_access.roles`, `cognito:groups`).
   - `REQ-SEC-03`: Kiểm soát truy cập dựa trên vai trò nghiêm ngặt (RBAC): `ROLE_OPERATOR` có quyền đọc/ghi nghiệp vụ; `ROLE_ADMIN` quản trị hệ thống.
   - `REQ-SEC-04`: Bảo vệ tuyệt đối các endpoint giám sát Actuator (chỉ cho phép truy cập cục bộ localhost hoặc người dùng có `ROLE_ADMIN`).
   - `REQ-SEC-05`: Cách ly môi trường kiểm thử (H2 Console bị khóa chặt trên Production, chỉ kích hoạt khi bật profile `dev`/`test` với tài khoản xác thực).
   - `REQ-SEC-06`: Ngăn chặn tấn công Clickjacking (`X-Frame-Options: SAMEORIGIN`) và CSRF Protection phù hợp cho Stateless REST API.

4. **Trụ Cột 4: Khả Năng Chống Chịu & Vận Hành (Resilience & Observability - REQ-OPS)**
   - `REQ-OPS-01`: Giới hạn tần suất gọi API (Rate Limiting) thuật toán Token Bucket (60 req/phút, burst 10) phản hồi mã lỗi `429 Too Many Requests` và header `Retry-After`.
   - `REQ-OPS-02`: Cơ chế giải phóng bộ nhớ tự động (Cache Eviction) bằng Caffeine LRU Cache (giới hạn tối đa 10,000 buckets, tự giải phóng sau 10 phút không hoạt động) chống rò rỉ bộ nhớ (Out Of Memory).
   - `REQ-OPS-03`: Truy vết phân tán qua Correlation ID (`X-Correlation-Id`), tự động khởi tạo nếu thiếu, đồng bộ vào MDC Log Context cho mọi dòng log và trả lại response header.

5. **Trụ Cột 5: Chuẩn Mực Mã Nguồn & Thiết Kế Phần Mềm (Clean Architecture & Coding Rules - REQ-CODE)**
   - `REQ-CODE-01`: Phân tầng Clean Architecture 3 lớp độc lập: Controller (Giao tiếp) -> Service (Điều phối) -> Domain/Repository (Nghiệp vụ).
   - `REQ-CODE-02`: Bất biến hóa dữ liệu truyền tải (Data Immutability) 100% bằng Java 17 Records cho DTOs (`WorkOrderRequest`, `WorkOrderResponse`, `WorkOrderStatusRequest`, `PagedResponse`).
   - `REQ-CODE-03`: Tuân thủ quy chuẩn Java Enterprise Oracle: Độ dài mỗi dòng mã nguồn không vượt quá 120 ký tự (0 line violations).
   - `REQ-CODE-04`: Bản địa hóa 100% Javadoc và chú thích mã nguồn (Comments) sang Tiếng Việt chuẩn mực kỹ thuật, tường minh và chính xác.
   - `REQ-CODE-05`: Đóng gói cơ sở dữ liệu độc lập với Flyway Database Migration, hỗ trợ cả H2 và PostgreSQL.

6. **Trụ Cột 6: Kim Tự Tháp Kiểm Thử & Chốt Chặn Phủ Mã (Automated Testing & Quality Gate - REQ-TST)**
   - `REQ-TST-01`: Kim tự tháp kiểm thử đa tầng: Kiểm thử đơn vị (Unit Tests), Kiểm thử cắt lớp Web (`@WebMvcTest`), Kiểm thử cắt lớp Dữ liệu (`@DataJpaTest`), Kiểm thử Bảo mật (`@SpringBootTest` Security), và Kiểm thử Tích hợp toàn diện (`IntegrationTest`).
   - `REQ-TST-02`: Đạt chỉ số kiểm thử tuyệt đối: **117/117 tests Green** (0 failures, 0 errors).
   - `REQ-TST-03`: Chốt chặn JaCoCo Quality Gate nghiêm ngặt: **100% Line Coverage (156/156 lines)** và **100% Branch Coverage (17/17 branches)** cho các gói nghiệp vụ cốt lõi.
   - `REQ-TST-04`: Áp dụng Test Fixture Pattern chuẩn mực (`WorkOrderTestFixtures`) loại bỏ hoàn toàn mã kiểm thử trùng lặp (DRY).

7. **Trụ Cột 7: Đóng Gói Vận Hành & CI/CD (Containerization & DevOps - REQ-OPS)**
   - `REQ-OPS-04`: Đóng gói Docker Multi-stage tối ưu dung lượng hình ảnh (< 250MB), vận hành bằng người dùng phi đặc quyền (non-root user `omsuser:omsgroup` UID/GID 10001).
   - `REQ-OPS-05`: Thiết lập môi trường Docker Compose tích hợp sẵn PostgreSQL 16 và Keycloak 24 IAM.
   - `REQ-OPS-06`: Tự động hóa kiểm định chất lượng liên tục qua GitHub Actions CI/CD Pipeline (`.github/workflows/ci.yml`).

8. **Trụ Cột 8: Quản Trị Quy Trình & Truy Vết (Governance & Traceability - REQ-GOV)**
   - `REQ-GOV-01`: Toàn bộ tài liệu đặc tả kỹ thuật và mã nguồn đạt trạng thái đồng bộ tuyệt đối (Zero Spec Drift).
   - `REQ-GOV-02`: Mọi thay đổi mã nguồn đều gắn liền với GitHub Issue, quy chuẩn nhánh `feature/WO-<id>-<name>` và Pull Request có mô tả đầy đủ.
   - `REQ-GOV-03`: Bảng đối soát truy vết 100% từ Issue đến Pull Request và Commit trong `CHANGELOG.md`.
   - `REQ-GOV-04`: Sẵn sàng cho việc bàn giao và nghiệm thu dự án doanh nghiệp với hệ thống tài liệu song ngữ / tiếng Việt chuẩn hóa.

---

### Giai Đoạn 2: Xây Dựng Ma Trận Truy Vết Yêu Cầu Kỹ Thuật (TRTM Table)
Tạo bảng ma trận kiểm định toàn diện (Technical Requirements Traceability Matrix) liên kết chặt chẽ từng yêu cầu với hiện thực hóa thực tế:

| Mã Yêu Cầu (Req ID) | Tên Yêu Cầu & Ràng Buộc Kỹ Thuật | Vị Trí Mã Nguồn Hiện Thực (Source Code) | Vị Trí Kiểm Thử Tự Động (Automated Test) | Cột Mốc Changelog & PR | Bằng Chứng / Tiêu Chí Nghiệm Thu (Review Benchmark) |
|---|---|---|---|---|---|
| `REQ-DOM-01` | Aggregate Root `WorkOrder` bất biến | `src/main/java/.../WorkOrder.java` | `.../WorkOrderTest.java` | v1.0.0 (PR #10) | ID & Timestamps không thể sửa, constructor private validation |
| ... | ... | ... | ... | ... | ... |

*(Yêu cầu liệt kê đầy đủ tất cả các yêu cầu từ `REQ-DOM-01` đến `REQ-GOV-04` không bỏ sót bất kỳ hạng mục nào)*

---

### Giai Đoạn 3: Thiết Lập Bộ Tiêu Chí Review Dự Án Phân Tầng (3-Tier Review Checklist)
Cung cấp khung câu hỏi và tiêu chí kiểm định dành riêng cho 3 đối tượng người đánh giá dự án:

1. **Tier 1 - Dành cho Lập Trình Viên Review Mã Nguồn (Code Reviewer):**
   - [ ] Mã nguồn có tuân thủ độ dài dòng <= 120 ký tự không?
   - [ ] Các DTO có được thiết kế bằng `record` bất biến không?
   - [ ] Javadoc có đầy đủ thẻ mô tả, `@param`, `@return`, `@throws` bằng tiếng Việt chuẩn xác không?
   - [ ] Có bất kỳ khối `try-catch` nuốt lỗi hoặc log thiếu ngữ cảnh không?
2. **Tier 2 - Dành cho Kiến Trúc Sư Hệ Thống (Software Architect):**
   - [ ] Ranh giới phân tầng Clean Architecture có bị vi phạm (Domain không phụ thuộc Web/DB) không?
   - [ ] Máy trạng thái có bảo toàn tính một chiều `OPEN -> IN_PROGRESS -> DONE` không?
   - [ ] Định dạng lỗi RFC 7807 có tuân thủ tuyệt đối cấu trúc chuẩn không?
   - [ ] Bộ lọc Rate Limiting có khả năng chống cạn kiệt tài nguyên (OOM) thông qua Caffeine Cache Eviction không?
3. **Tier 3 - Dành cho Chuyên Viên Bảo Mật & Đảm Bảo Chất Lượng (Security & QA Auditor):**
   - [ ] Toàn bộ 117/117 automated tests có vượt qua 100% không?
   - [ ] Tỷ lệ bao phủ kiểm thử JaCoCo có đạt 100% Line & Branch Coverage không?
   - [ ] Các endpoint Actuator và H2 Console có được bảo vệ nghiêm ngặt khỏi truy cập trái phép không?
   - [ ] Phân quyền RBAC giữa `ROLE_OPERATOR` và `ROLE_ADMIN` có được kiểm thử xác thực không?

---

# CONSTRAINTS:
1. **Fact-based & Zero Hallucination:** Mọi yêu cầu kỹ thuật, tên class, đường dẫn file, tham số API, mã lỗi RFC 7807 trong ma trận bắt buộc phải tồn tại chính xác trong mã nguồn dự án hiện hành và `CHANGELOG.md`. Không tự ý sáng tác thêm các tính năng chưa có.
2. **Song Ngữ Chuẩn Hóa:** Tiêu đề và mã yêu cầu viết bằng quy chuẩn kỹ thuật quốc tế (`REQ-DOM-01`), nội dung giải thích bằng tiếng Việt chuyên ngành chính xác, mạch lạc, dễ hiểu.
3. **Traceability 100%:** Mọi mã yêu cầu đều phải có ít nhất 1 file source code, 1 file test class, và 1 mốc phát hành trong `CHANGELOG.md`.
4. **Không Sửa Code Logic:** Quá trình sinh đặc tả yêu cầu không làm biến đổi bất kỳ logic thực thi nào của mã nguồn Java.

---

# DONE WHEN:
1. Tạo thành công tệp tài liệu `docs/02-technical-requirements-and-review-matrix.md` hoàn chỉnh, sắc nét, có tính ứng dụng cao.
2. Bao phủ đủ 8 trụ cột kỹ thuật với tối thiểu 30 yêu cầu kỹ thuật chi tiết (`REQ-DOM-xx`, `REQ-API-xx`, `REQ-SEC-xx`, `REQ-OPS-xx`, `REQ-CODE-xx`, `REQ-TST-xx`, `REQ-GOV-xx`).
3. Bảng Ma trận Truy vết Yêu cầu Kỹ thuật (TRTM) hoàn chỉnh 100%, liên kết chính xác với từng file trong `src/main/java` và `src/test/java`.
4. Bộ checklist kiểm định 3 cấp độ (Code - Architecture - Security/QA) đầy đủ các tiêu chuẩn đo lường phục vụ việc review và nghiệm thu dự án.
5. Tích hợp tệp prompt này vào `00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md` và `docs/03-CONTEXT_INDEX.md`.
```
