# Prompt Giai Đoạn 13: Kiểm Tra & Triển Khai Bộ Quy Chuẩn Coding Rules (Oracle Senior Java & Design Patterns) Cho Toàn Bộ Dự Án & Markdown Specs

```markdown
# ROLE:
Bạn là một "Principal Java Software Architect" kiêm "Lead Code Quality Compliance Auditor" với hơn 15 năm kinh nghiệm về Thiết kế Kiến trúc Hệ thống Java Enterprise, Nguyên lý Effective Java (Joshua Bloch), Clean Architecture trên nền tảng Spring Boot 3.3 / Java 17 LTS, và chuẩn mực mã nguồn của Oracle Core Platform & OpenJDK Team. Mục tiêu tối thượng của bạn là thực hiện cuộc tổng rà soát (Comprehensive Audit) và triển khai áp dụng đồng bộ bộ quy chuẩn kỹ thuật trong `docs/00-coding-rules.md` lên toàn bộ mã nguồn Java (`src/main/`, `src/test/`) và toàn bộ các tệp tài liệu đặc tả/bản thảo Markdown (`docs/`, `docs/drafts/`, `.github/`), đảm bảo hệ thống đạt chuẩn mực thiết kế tối ưu của một Senior Java Engineer tại Oracle, chuyên sử dụng các Design Pattern để tối ưu hóa cấu trúc class, bộ nhớ và khả năng bảo trì.

---

# TASK:
Thực hiện quy trình kiểm tra, tái cấu trúc và đồng bộ hóa toàn diện theo 5 bước tuần tự:

### Bước 1: Rà Soát & Thiết Lập Ma Trận Tuân Thủ (Compliance Audit & Gap Analysis)
1. **Kiểm tra mã nguồn Java (`src/main/java/` và `src/test/java/`):**
   - Đối chiếu từng file Java với 7 trụ cột trong `docs/00-coding-rules.md`:
     * *Immutability & Encapsulation:* Có sử dụng `final` cho fields và biến cục bộ không đổi? Có để lộ setter công khai trên Aggregate Root không?
     * *Design Patterns:* DTOs và Value Objects đã có Static Factory Method (`from()`, `of()`) chưa? State Machine đã đóng gói trong Enum/State pattern (`canTransitionTo()`) chưa?
     * *Dependency Injection:* Có class nào vi phạm dùng Field Injection `@Autowired` không? 100% đã dùng Constructor Injection với `private final` fields chưa?
     * *JVM & GC Optimization:* Có chỗ nào ghép chuỗi bằng toán tử `+` trong vòng lặp lớn thay vì `StringBuilder`? Các Collections (`ArrayList`, `HashMap`) đã được pre-sizing với `initialCapacity` khi biết trước kích thước chưa?
     * *Logging:* Có lệnh log nào dùng string concatenation thay vì SLF4J parametric `{}` không? Có log PII hay raw request payload không?
     * *Zero-Lombok:* Đảm bảo tuyệt đối không có bất kỳ annotation Lombok nào.
     * *Zero Magic Numbers & Literal Strings:* Có số nguyên thô (magic numbers: 20, 60, 401, 429, 10000, 10m) trực tiếp trong logic điều kiện, bộ lọc rate limit, vòng lặp, kiểm tra trạng thái hay annotations không? Có literal strings phân tán (metric names, tag keys, role names, JSON error keys, media types, boundary length limits) không? 100% đã được gom vào `public static final` constants, enums, hoặc `@ConfigurationProperties` chưa?
2. **Kiểm tra toàn bộ tài liệu Markdown (`docs/`, `docs/drafts/`, `.github/`):**
   - Rà soát toàn bộ các code snippet Java mẫu trong:
     * `.github/copilot-instructions.md`
     * `docs/drafts/*.md` (`draft-workorder-domain.md`, `draft-workorder-service.md`, `draft-dtos.md`, `draft-global-exception-handler.md`, `draft-file-mapping.md`, `draft-workorder-*.md`)
     * `docs/00-internal-coding-standards.md`, `docs/01-domain-model.md`, `docs/02-api-spec.md`
   - Phát hiện các đoạn code mẫu cũ còn dùng constructor thô, thiếu static factory methods, dùng reflection mapping, chứa magic numbers/literal strings hoặc chưa thể hiện đúng các Design Pattern theo chuẩn Oracle.
3. **Lập bảng Ma Trận Tuân Thủ (Coding Rules Compliance Matrix):**
   - Liệt kê: `Tệp cần xử lý` | `Hạng mục quy chuẩn` | `Hiện trạng` | `Hành động khắc phục`.

### Bước 2: Triển Khai & Chuẩn Hóa Mã Nguồn Java (`src/`)
Thực hiện các chỉnh sửa tối ưu hóa class theo đúng Design Patterns mà không làm thay đổi logic nghiệp vụ:
1. **Static Factory Method Pattern:**
   - Đảm bảo các DTOs (như `WorkOrderResponse`) có static factory method chuẩn mực: `public static WorkOrderResponse from(WorkOrder workOrder)` kiểm tra `Objects.requireNonNull()`.
2. **State Pattern & Invariant Encapsulation:**
   - Xác nhận Entity Domain (`WorkOrder`) bảo vệ toàn vẹn tính bất biến qua `advanceStatus(WorkOrderStatus target)`, gọi `status.canTransitionTo(target)` và ném `IllegalStateException` khi vi phạm. Tuyệt đối không cung cấp `setStatus()`.
3. **Tối Ưu Hóa Bộ Nhớ & JIT Escape Analysis:**
   - Bổ sung từ khóa `final` cho các biến cục bộ không đổi trong các phương thức Service và Controller.
   - Thêm `initialCapacity` khi khởi tạo `ArrayList` hoặc `HashMap` nếu số lượng phần tử đã được xác định trước.
4. **Chuẩn Hóa SLF4J Parametric Logging:**
   - Thay thế toàn bộ các lệnh `log.info("..." + var)` bằng `log.info("...", var)`.
5. **Giữ Vững Pure Java 17 Records & Constructor Injection:**
   - Xác nhận 100% DTOs là `record` bất biến, 100% Spring components dùng Constructor Injection với `private final` fields.
6. **Triệt Tiêu 100% Magic Numbers & Literal Strings (Zero Magic Values Refactoring):**
   - Chuyển đổi toàn bộ số thô (status codes, cache duration/capacity, rate limiting limits) thành hằng số tự mô tả hoặc externalize qua `@ConfigurationProperties` trong `application.yml`.
   - Gom toàn bộ tên Micrometer Metrics và Tag Keys vào lớp hằng số tập trung `WorkOrderMetrics`.
   - Gom toàn bộ chuỗi vai trò người dùng vào `RoleConstants` (kèm tiền tố `ROLE_`).
   - Gom các khóa RFC 7807 problem details (`invalidParams`, `name`, `reason`, `type`) vào `ProblemTypes` hoặc hằng số chuyên biệt.
   - Dùng chung hằng số độ dài tối đa/tối thiểu cho JPA Entity và Bean Validation.
   - Bắt buộc dùng `MediaType.APPLICATION_PROBLEM_JSON_VALUE` và `HttpHeaders.RETRY_AFTER` thay vì literal strings.

### Bước 3: Đồng Bộ & Nâng Cấp Toàn Bộ Các File Markdown Đã Sinh (`docs/` & `.github/`)
1. **Cập nhật `.github/copilot-instructions.md`:**
   - Bổ sung trực tiếp bộ quy chuẩn Oracle Senior Java Engineer: Bắt buộc dùng Static Factory Methods, State/Strategy pattern cho State Machine, Pre-sizing Collections, cấm reflection mapper, cấm Lombok, bắt buộc Constructor Injection, và cấm 100% Magic Numbers & Literal Strings.
2. **Cập nhật `docs/00-internal-coding-standards.md`:**
   - Đồng bộ hoàn toàn với `docs/00-coding-rules.md` về nguyên tắc Effective Java, kiến trúc 3 tầng, danh mục Design Pattern, và chính sách quản lý hằng số tập trung (Centralized Constants & Zero Magic Values).
3. **Cập nhật toàn bộ các file trong `docs/drafts/`:**
   - Cập nhật các khối code mẫu trong `docs/drafts/draft-dtos.md`, `draft-workorder-domain.md`, `draft-workorder-service.md`, `draft-global-exception-handler.md` để đảm bảo mọi đoạn code blueprint mà Copilot tham khảo đều tuân thủ 100% chuẩn Oracle Senior Java Engineer, ứng dụng đúng Design Patterns và không chứa magic numbers/literal strings.

### Bước 4: Kiểm Thử Tự Động & Thẩm Định 100% JaCoCo Coverage Gate
1. Chạy lệnh kiểm thử toàn diện: `mvn clean verify`.
2. Xác nhận:
   - Toàn bộ test suite chạy thành công 100% (**GREEN**), 0 Failures, 0 Errors, 0 Skipped.
   - Không gây ra bất kỳ lỗi hồi quy nào (Zero Regression).
   - Ngưỡng JaCoCo Quality Gate vẫn đạt tuyệt đối **100% Line Coverage** và **100% Branch Coverage** trên toàn bộ 11 monitored classes.

### Bước 5: Báo Cáo Nghiệm Thu & Đóng Gói Atomic Pull Request
1. Lập báo cáo tổng kết chi tiết:
   - Danh sách các class Java đã được tối ưu hóa cấu trúc và áp dụng Design Pattern.
   - Danh sách các file Markdown đã được đồng bộ code mẫu.
   - Bằng chứng kiểm thử tự động và kết quả JaCoCo Coverage.
2. Tạo nhánh `feature/WO-enforce-oracle-coding-rules` và mở Pull Request theo mẫu chuẩn `.github/PULL_REQUEST_TEMPLATE.md`.

---

# CONSTRAINTS:
1. **Zero Behavioral Regression:** Mọi tối ưu hóa class và áp dụng Design Pattern chỉ nhằm nâng cao tính bao đóng, tính tường minh và hiệu năng bộ nhớ; tuyệt đối không thay đổi giao ước API (API contracts), mã HTTP status hay cấu trúc JSON trả về.
2. **Strict 100% Coverage Preservation:** Tỷ lệ Line Coverage và Branch Coverage sau khi tái cấu trúc bắt buộc phải duy trì ở mức 100.0%. Tuyệt đối không hạ thấp ngưỡng kiểm soát trong `pom.xml`.
3. **Zero Lombok & Pure Java 17:** Kiên định 100% không sử dụng Lombok. DTOs bắt buộc dùng Java 17 records.
4. **Zero Magic Values:** Tuyệt đối không để sót bất kỳ magic number hoặc literal string tự do nào trong mã nguồn logic và filters.
5. **Single Source of Truth:** Mọi code snippet trong `docs/drafts/` và `docs/` phải khớp hoàn toàn với quy chuẩn thực tế, không để lại bất kỳ đoạn code mẫu nào vi phạm `docs/00-coding-rules.md`.

---

# DONE WHEN:
1. Bảng Ma Trận Tuân Thủ Coding Rules được lập hoàn chỉnh, ghi nhận 100% các tệp Java và Markdown đều ở trạng thái **COMPLIANT**.
2. Toàn bộ mã nguồn Java trong `src/` áp dụng triệt để phong cách Oracle Senior Java Engineer và các Design Patterns tương ứng.
3. Toàn bộ các tệp Markdown (`docs/drafts/*.md`, `docs/00-internal-coding-standards.md`, `.github/copilot-instructions.md`) được cập nhật đồng bộ các đoạn code mẫu đạt chuẩn.
4. Lệnh `mvn clean verify` chạy thành công với 100% bài test PASS và JaCoCo đạt 100% Line & Branch Coverage.
5. Atomic Pull Request được mở thành công trên GitHub sẵn sàng để Human Reviewer phê duyệt.
```
