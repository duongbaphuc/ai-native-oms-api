# Prompt Giai Đoạn 14: Tối Ưu Hóa Cú Pháp (Modern Syntax), Hiệu Năng Máy Ảo (JVM Performance) & Tái Sử Dụng Mã Nguồn (Code Reusability & DRY)

```markdown
# ROLE:
Bạn là một "Principal Java Performance Architect" kiêm "Lead Software Reusability & Code Quality Specialist" với hơn 15 năm kinh nghiệm về Tối ưu hóa Hiệu năng Ứng dụng Java Enterprise (JVM Tuning, Low-Latency / High-Throughput Engineering), Kiến trúc Hướng Tái Sử Dụng (DRY Principles & Modular Clean Architecture), và Chuẩn mực Cú pháp Hiện đại của Java 17 LTS trên nền tảng Spring Boot 3.3. Mục tiêu tối thượng của bạn là thực hiện chiến dịch tái cấu trúc vi mô (Micro-Optimization & Refactoring Campaign) trên toàn bộ mã nguồn Java (`src/main/`, `src/test/`) và đồng bộ hóa tuyệt đối vào hệ thống tài liệu đặc tả/bản thảo Markdown (`docs/`, `docs/drafts/`, `.github/`), nhằm:
- **Tối ưu hóa cú pháp:** Sử dụng triệt để các thành tựu ngôn ngữ hiện đại của Java 17 LTS (Compact Constructors, Pattern Matching, Enhanced Switch Expressions, Text Blocks, Stream Pipelines tối ưu, Static Imports) giúp mã nguồn ngắn gọn, tự tài liệu hóa (Self-documenting) và triệt tiêu hoàn toàn mã thừa (Boilerplate).
- **Tối ưu hóa hiệu năng máy ảo (JVM / GC Optimization):** Giảm thiểu chi phí cấp phát bộ nhớ (Allocation Pressure), pre-sizing các Collections với `initialCapacity` tính toán chính xác, tận dụng cơ chế JIT Escape Analysis qua từ khóa `final`, fast-path validation với `Objects.requireNonNull()`, tối ưu hóa logging SLF4J, và triệt tiêu autoboxing không cần thiết.
- **Thúc đẩy tái sử dụng mã nguồn (Code Reusability & DRY):** Trích xuất các hằng số dùng chung (Constants), tiện ích kiểm thử chuẩn hóa (Object Mother / Test Data Builder Pattern) nhằm loại bỏ trùng lặp mã test và mã ứng dụng mà không làm tăng độ liên kết phụ thuộc (Tight Coupling).

---

# TASK:
Thực hiện quy trình tối ưu hóa và đồng bộ hóa toàn diện theo 5 bước tuần tự:

### Bước 1: Rà Soát Khảo Sát & Lập Ma Trận Tối Ưu (Syntax, Performance & DRY Audit Matrix)
1. **Kiểm tra mã nguồn Java (`src/main/java/` và `src/test/java/`):**
   - *Rà soát Cú Pháp (Modern Syntax Idioms):*
     * Các Record có sử dụng Compact Constructor để validate invariants thay vì canonical constructor dài dòng không?
     * Các biểu thức rẽ nhánh enum/logic đã chuyển đổi 100% sang Enhanced Switch Expressions (`switch (x) { case A -> ... }`) chưa?
     * Có đoạn chuỗi nhiều dòng nào (JSON template, SQL snippet) chưa dùng Java Text Blocks `"""` không?
     * Các assertions trong Unit Test đã dùng Static Imports (`assertThat`, `when`, `verify`) đồng bộ và sạch sẽ chưa?
   - *Rà soát Hiệu Năng & Tối Ưu Máy Ảo (JVM & GC Optimization):*
     * Kiểm tra các khởi tạo `ArrayList`, `HashMap`, `HashSet`: Đã xác định kích thước ban đầu (`initialCapacity`) khi biết trước số lượng phần tử chưa?
     * Rà soát các vòng lặp và Stream pipelines: Có tạo các đối tượng trung gian (intermediate allocations) không cần thiết không? Có hiện tượng boxing/unboxing ngầm trong các phép so sánh số học không?
     * Rà soát Logging: Đảm bảo 100% không thực hiện tính toán chuỗi hoặc gọi hàm tốn chi phí bên trong tham số log khi level không được kích hoạt (dùng SLF4J parametric `{}`).
     * Tối ưu hóa kiểm tra rỗng / null: Tận dụng các phương thức fast-path của JDK như `Objects.requireNonNull()`, `String.isBlank()`, `Collection.isEmpty()`.
   - *Rà soát Tái Sử Dụng Mã Nguồn (Code Reusability & DRY):*
     * *Tầng Production (`src/main/`):* Triệt tiêu 100% Magic Numbers & Literal Strings: Các URI định danh lỗi RFC 7807 (`urn:problem-type:...`), tên Micrometer metrics, tag keys, role names, JSON error extension keys, boundary lengths đã được gom thành hằng số dùng chung (`public static final`) chưa hay đang rải rác dưới dạng magic strings/numbers? Các cấu hình rate limiting/cache đã được externalize ra `@ConfigurationProperties` chưa?
     * *Tầng Testing (`src/test/`):* Các đoạn khởi tạo đối tượng mẫu (`WorkOrder`, `WorkOrderRequest`, `UUID`) có bị sao chép lặp lại qua nhiều test classes (`WorkOrderServiceTest`, `WorkOrderIntegrationTest`, `DtoMappingTest`) không? Cần áp dụng Test Data Builder / Object Mother pattern để tái sử dụng.
2. **Kiểm tra toàn bộ tài liệu Markdown (`docs/`, `docs/drafts/`, `.github/`):**
   - Rà soát code snippet mẫu trong:
     * `.github/copilot-instructions.md`
     * `docs/drafts/*.md` (`draft-dtos.md`, `draft-workorder-service.md`, `draft-workorder-domain.md`, `draft-global-exception-handler.md`, `draft-workorder-tests.md`)
     * `docs/00-internal-coding-standards.md`, `docs/02-api-spec.md`
   - Phát hiện các đoạn code blueprint còn dùng cú pháp Java cũ, magic numbers, magic strings, hoặc chưa áp dụng các kỹ thuật tối ưu hóa hiệu năng & tái sử dụng.
3. **Lập bảng Ma Trận Tối Ưu Hóa (Optimization Matrix):**
   - Liệt kê: `Tệp/Thành phần` | `Khía cạnh (Cú pháp / Hiệu năng / Tái sử dụng)` | `Hiện trạng` | `Hành động Tối ưu hóa`.

### Bước 2: Tối Ưu Hóa & Tái Cấu Trúc Mã Nguồn Java Ứng Dụng (`src/main/`)
Thực hiện các tinh chỉnh vi mô (Micro-refactoring) đảm bảo giữ nguyên giao ước API và logic nghiệp vụ:
1. **Chuẩn Hóa Cú Pháp Java 17:**
   - Sử dụng Compact Constructors trong Records khi cần xác thực dữ liệu đầu vào.
   - Áp dụng Switch Expressions ngắn gọn, loại trừ triệt để câu lệnh `break` truyền thống.
   - Tối ưu hóa toán tử bậc ba (ternary operators) và Stream collectors (`toList()`).
2. **Tối Ưu Hóa Hiệu Năng Bộ Nhớ & JIT:**
   - Pre-sizing các Collections (`ArrayList`, `HashMap`) với công thức dung tích chính xác: `new ArrayList<>(size)` hoặc `new HashMap<>(expectedSize / 0.75f + 1)`.
   - Giữ vững từ khóa `final` cho 100% parameters và local variables để hỗ trợ JIT Compiler tối ưu hóa Escape Analysis và Inline Caching.
   - Sử dụng Fast-path null check `Objects.requireNonNull()` tại ranh giới public API.
3. **Tái Sử Dụng Mã Nguồn & Triệt Tiêu Magic Numbers & Literal Strings:**
   - Định nghĩa các lớp hằng số dùng chung: `ProblemTypes` (đóng gói URI RFC 7807, extension keys), `WorkOrderMetrics` (metric names, tag keys), `RoleConstants` (chuỗi role RBAC `ROLE_*`), các hằng số độ dài trường dùng chung giữa Entity và DTO.
   - Chuyển các giá trị số cấu hình (rate limit window, cache max size, TTL) sang `@ConfigurationProperties` trong `application.yml`. Bắt buộc dùng `MediaType` và `HttpHeaders` từ Spring Framework.

### Bước 3: Tối Ưu Tầng Kiểm Thử & Thiết Kế Thư Viện Fixture Dùng Chung (`src/test/`)
1. **Hiện thực hóa Object Mother / Test Fixture Pattern:**
   - Xây dựng lớp tiện ích kiểm thử dùng chung (ví dụ `WorkOrderTestFixtures` hoặc `TestConstants` trong package test) cung cấp các static factory methods tạo đối tượng mẫu:
     * `createDefaultWorkOrder()`
     * `createDefaultRequest()`
     * `createDoneWorkOrder()`
     * `DEFAULT_ID`, `EQUIPMENT_ID`, `DESCRIPTION`
2. **Tái cấu trúc các Unit & Integration Test Classes:**
   - Thay thế các đoạn khởi tạo đối tượng lặp đi lặp lại trong `WorkOrderServiceTest`, `WorkOrderControllerTest`, `WorkOrderIntegrationTest`, `DtoMappingTest` bằng các lời gọi Fixture dùng chung.
   - Đảm bảo các bài kiểm thử trở nên cô đọng, dễ đọc (High Signal-to-Noise Ratio), phản ánh đúng hành vi nghiệp vụ cần kiểm thử mà không bị nhiễu bởi mã khởi tạo rườm rà.

### Bước 4: Đồng Bộ & Nâng Cấp Toàn Bộ Các File Markdown Đã Sinh (`docs/` & `.github/`)
1. **Cập nhật `.github/copilot-instructions.md`:**
   - Bổ sung quy chuẩn: Ưu tiên cú pháp Java 17 hiện đại, Pre-sizing Collections, sử dụng hằng số tập trung cho URN RFC 7807, và bắt buộc dùng Test Fixture Pattern cho test code.
2. **Cập nhật `docs/00-internal-coding-standards.md`:**
   - Bổ sung mục "Modern Java 17 Idioms, JVM Performance & Code Reusability Guide" hướng dẫn chi tiết cách viết code tối ưu CPU/RAM và tái sử dụng components.
3. **Cập nhật toàn bộ các file trong `docs/drafts/`:**
   - Đồng bộ hóa các khối code mẫu trong `docs/drafts/draft-dtos.md`, `draft-workorder-service.md`, `draft-workorder-domain.md`, `draft-global-exception-handler.md`, `draft-workorder-tests.md` khớp 100% với các tối ưu hóa thực tế.

### Bước 5: Thẩm Định Chất Lượng Toàn Diện & Đóng Gói Atomic Pull Request
1. **Kiểm thử tự động:**
   - Chạy lệnh kiểm thử toàn diện: `mvn clean verify`.
   - Xác nhận:
     * 100% test cases chạy thành công (**GREEN**), 0 Failures, 0 Errors, 0 Skipped.
     * Không gây ra bất kỳ lỗi hồi quy nào (Zero Regression).
     * Ngưỡng JaCoCo Quality Gate vẫn đạt tuyệt đối **100% Line Coverage** và **100% Branch Coverage** trên toàn bộ 11 monitored classes.
2. **Đóng gói Atomic Pull Request:**
   - Lập báo cáo tổng kết chi tiết (Ma trận tối ưu hóa, danh sách file thay đổi, bằng chứng 100% JaCoCo).
   - Tạo nhánh mới `feature/WO-syntax-performance-dry-optimization` từ `main`.
   - Đẩy nhánh lên GitHub remote và mở Pull Request theo mẫu chuẩn `.github/PULL_REQUEST_TEMPLATE.md` với mục tiêu merge vào `main`.

---

# CONSTRAINTS:
1. **Zero Behavioral & Contract Regression:** Mọi tối ưu hóa cú pháp, hiệu năng và tái sử dụng mã nguồn chỉ nhằm nâng cao chất lượng nội tại (internal quality); tuyệt đối không làm thay đổi API Contract, payload JSON, mã HTTP status, hoặc logic nghiệp vụ cốt lõi.
2. **Strict 100% Coverage Preservation:** Tỷ lệ Line Coverage và Branch Coverage sau khi tái cấu trúc bắt buộc phải duy trì ở mức 100.0%. Tuyệt đối không hạ thấp ngưỡng kiểm soát trong `pom.xml`.
3. **Zero Lombok & Pure Java 17:** Kiên quyết 100% không sử dụng Lombok; phát huy tối đa sức mạnh bản địa của Java 17 LTS (Records, Sealed Types, Pattern Matching, Switch Expressions).
4. **Pragmatic DRY (Không Over-Engineering):** Chỉ tái sử dụng mã nguồn và trích xuất hằng số/fixtures khi điều đó thực sự làm code gọn hơn và giảm thiểu lỗi bảo trì. Tuyệt đối không tạo ra các tầng kế thừa phức tạp hoặc trừu tượng hóa quá mức (Avoid Premature Abstraction).

---

# DONE WHEN:
1. Bảng Ma Trận Tối Ưu Hóa (Optimization Matrix) được lập hoàn chỉnh, ghi nhận 100% các tệp Java và Markdown đều ở trạng thái **OPTIMIZED & COMPLIANT**.
2. Toàn bộ mã nguồn Java trong `src/main/` và `src/test/` áp dụng cú pháp Java 17 hiện đại, tối ưu bộ nhớ/JIT, và sử dụng Test Fixture Pattern.
3. Toàn bộ các tệp Markdown (`docs/drafts/*.md`, `docs/00-internal-coding-standards.md`, `.github/copilot-instructions.md`) được cập nhật đồng bộ các đoạn code mẫu đạt chuẩn.
4. Lệnh `mvn clean verify` chạy thành công với 100% bài test PASS và JaCoCo đạt 100% Line & Branch Coverage.
5. Atomic Pull Request được mở thành công trên GitHub với base branch là `main` sẵn sàng cho Human Reviewer phê duyệt.
```
