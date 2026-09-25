<!--
Role: Principal Quality Assurance Architect & Lead Code Auditor
Task: Kiểm toán toàn diện mã nguồn dự án theo 13 tiêu chí chất lượng
Context files:
  - docs/01-br-analysis-wo.md (Yêu cầu nghiệp vụ gốc — Baseline SSOT)
  - docs/01-domain-model.md (Mô hình miền, Invariants, State Machine)
  - docs/02-api-spec.md (Hợp đồng API, RBAC, Step-by-step Logic)
  - docs/02-database-migration-spec.md (Schema CSDL, Flyway DDL)
  - docs/02-security-auth-spec.md (Dual SecurityFilterChain, JWT, Rate Limit)
  - docs/02-observability-and-logging.md (Logging, Tracing, Metrics)
  - docs/00-coding-rules.md (Chuẩn viết mã Java 17 / Spring Boot)
  - docs/00-api-rules.md (Chuẩn RESTful API, RFC 7807)
  - docs/00-internal-coding-standards.md (Chuẩn nội bộ: UTC Instant, cấm Lombok)
  - docs/00-security-rules.md (OWASP API, Least Privilege)
  - docs/drafts/ (12 bản thảo Blueprint — nguồn chân lý thiết kế)
Target audit scope:
  - 19 production Java files: src/main/java/com/gpc/oms/**/*.java
  - 20 test Java files: src/test/java/com/gpc/oms/**/*.java
  - 1 Flyway migration: src/main/resources/db/migration/V1__init_work_orders_schema.sql
  - 1 application config: src/main/resources/application.yml
  - 1 web console: src/main/resources/static/index.html
  - Build config: pom.xml, Dockerfile, docker-compose.yml, .github/workflows/ci.yml
  - Git hygiene: .gitignore, .dockerignore, .copilotignore
Output: Báo cáo kiểm toán dạng Markdown ghi nhận PASS/FAIL cho từng tiêu chí, kèm bằng chứng dẫn chiếu cụ thể (file, dòng code, đoạn trích).
-->

# PROMPT: KIỂM TOÁN TOÀN DIỆN CHẤT LƯỢNG MÃ NGUỒN — 13 TIÊU CHÍ
## Dự án: Outage Work Order API (`oms-api-demo`) — Phiên bản v1.0.0

---

## MỤC TIÊU

Bạn đóng vai trò **Principal Quality Assurance Architect** — chuyên gia kiểm toán mã nguồn cấp cao. Nhiệm vụ của bạn là rà soát **TOÀN BỘ** mã nguồn dự án `ai-native-oms-api` theo **13 tiêu chí chất lượng** được liệt kê bên dưới. Mỗi tiêu chí phải được đánh giá **PASS** hoặc **FAIL**, kèm theo bằng chứng cụ thể (tên file, số dòng, đoạn mã trích dẫn, giải thích lý do).

---

## NGUYÊN TẮC KIỂM TOÁN BẮT BUỘC

1. **Đọc Yêu cầu Nghiệp vụ Gốc trước tiên:** Nạp `docs/01-br-analysis-wo.md` làm kim chỉ nam. Mọi đánh giá "dư thừa" hay "thiếu" phải đối chiếu ngược về tài liệu yêu cầu gốc này.
2. **Đối chiếu 3 nguồn chân lý:** Mỗi phát hiện phải được tam giác hóa (triangulation) giữa: (a) Yêu cầu nghiệp vụ gốc → (b) Đặc tả kỹ thuật `docs/02-*.md` → (c) Mã nguồn thực tế `src/`.
3. **Không suy diễn — Chỉ dẫn chứng:** Không được phỏng đoán hoặc giả định. Mọi nhận định PASS/FAIL đều phải kèm trích dẫn mã nguồn hoặc tài liệu cụ thể.
4. **Phân biệt Severity:** Mỗi phát hiện FAIL phải gắn mức nghiêm trọng: 🔴 `CRITICAL` (vi phạm nghiệp vụ, sai logic), 🟠 `MAJOR` (ảnh hưởng hiệu suất/bảo trì), 🟡 `MINOR` (mỹ thuật code, convention).

---

## 13 TIÊU CHÍ KIỂM TOÁN

### TIÊU CHÍ 1: CODE DƯ THỪA (Không Có Trong Yêu Cầu)

**Câu hỏi kiểm toán:** Có tồn tại class, method, field, endpoint, hoặc logic nào trong mã nguồn production mà KHÔNG được yêu cầu bởi tài liệu nghiệp vụ (`docs/01-br-analysis-wo.md`) và đặc tả kỹ thuật (`docs/02-api-spec.md`)?

**Hướng dẫn kiểm tra:**
- Đối chiếu danh sách 19 file production Java với `docs/drafts/draft-file-mapping.md`. Có file nào ngoài danh sách?
- Kiểm tra từng endpoint trong `WorkOrderController.java` — có endpoint nào không xuất hiện trong `docs/02-api-spec.md`?
- Kiểm tra từng method trong `WorkOrderService.java` — có method nào không được gọi từ Controller?
- Kiểm tra các field trong entity `WorkOrder.java` — có field nào không xuất hiện trong `docs/01-domain-model.md`?
- Đánh giá thư mục `csv-vat-calculator/` — có phải là thành phần của dự án OMS hay là code dư thừa?
- Kiểm tra `src/main/resources/static/index.html` — Web Console có nằm trong yêu cầu ban đầu hay không?

**Tiêu chuẩn PASS:** Không tồn tại production code nào mà không truy ngược được về ít nhất một yêu cầu nghiệp vụ hoặc đặc tả kỹ thuật đã được phê duyệt.

---

### TIÊU CHÍ 2: THUẬT TOÁN KHÔNG PHÙ HỢP (Ảnh hưởng Hiệu suất RAM/CPU)

**Câu hỏi kiểm toán:** Có thuật toán hoặc cấu trúc xử lý nào trong mã nguồn gây lãng phí tài nguyên RAM/CPU một cách không cần thiết?

**Hướng dẫn kiểm tra:**
- `RateLimitingFilter.java`: Kiểm tra cấu trúc dữ liệu lưu trữ bucket per IP (`ConcurrentHashMap`). Chiến lược eviction (dọn dẹp) có hiệu quả không? Có rủi ro bùng nổ bộ nhớ nếu có hàng triệu IP khác nhau?
- `WorkOrderService.java`: Phương thức `getAllWorkOrders()` có thực hiện phân trang đúng cách không? Có tồn tại truy vấn `findAll()` không phân trang nào không?
- `WorkOrderRepository.java`: Các custom query method có sử dụng index đúng cách không? Đối chiếu với `V1__init_work_orders_schema.sql` để kiểm tra index coverage.
- `CorrelationIdFilter.java`: MDC có được dọn dẹp đúng cách trong khối `finally` không? Có rủi ro rò rỉ ThreadLocal không?
- `SecurityConfig.java`: Cấu hình filter chain có tạo ra overhead không cần thiết cho mỗi request không?

**Tiêu chuẩn PASS:** Không tồn tại thuật toán O(n²) hoặc tệ hơn cho các thao tác thông thường; không có rủi ro memory leak; phân trang được thực hiện ở tầng query.

---

### TIÊU CHÍ 3: ÁP DỤNG KIỂU DỮ LIỆU KHÔNG PHÙ HỢP (Ảnh hưởng Hiệu suất & Bộ nhớ)

**Câu hỏi kiểm toán:** Có trường hợp nào kiểu dữ liệu được chọn không tối ưu, gây boxing/unboxing không cần thiết, hoặc lãng phí bộ nhớ?

**Hướng dẫn kiểm tra:**
- `WorkOrder.java`: Khóa chính `id` dùng kiểu `UUID` — có phù hợp với yêu cầu nghiệp vụ? Kiểm tra `@GeneratedValue(strategy = ...)`.
- `WorkOrder.java`: Các trường thời gian (`createdAt`, `resolvedAt`) có dùng `Instant` (UTC) đúng chuẩn `docs/00-internal-coding-standards.md` hay dùng `LocalDateTime`?
- DTOs (`WorkOrderRequest`, `WorkOrderResponse`): Có dùng Java Records hay POJO class? Records có phù hợp cho immutable DTO không?
- `PagedResponse.java`: Các field phân trang (`page`, `size`, `totalElements`) có dùng kiểu nguyên thủy `int`/`long` hay wrapper `Integer`/`Long`?
- `Priority.java` và `WorkOrderStatus.java`: Có dùng Enum đúng cách không? Enum có chứa state mutable nào không?
- `RateLimitingFilter.java`: Bucket4j capacity dùng kiểu `long` hay `Long`? Token refill interval có dùng `Duration` chuẩn không?

**Tiêu chuẩn PASS:** Tuân thủ nguyên tắc: dùng primitive cho tính toán cục bộ, dùng wrapper chỉ khi cần nullable; `Instant` cho timestamps UTC; Java Records cho DTOs immutable.

---

### TIÊU CHÍ 4: VIẾT TEST CASE KHÔNG ĐỦ CƠ SỞ ĐỂ TIN CẬY CHẤT LƯỢNG MÃ NGUỒN

**Câu hỏi kiểm toán:** Bộ kiểm thử 117 test cases có thực sự bao quát đủ các kịch bản nghiệp vụ, edge cases, và ranh giới bảo mật?

**Hướng dẫn kiểm tra:**
- **Happy Path Coverage:** Kiểm tra `WorkOrderIntegrationTest.java` — có test đầy đủ vòng đời `OPEN → IN_PROGRESS → DONE` không?
- **Negative / Edge Cases:** Có test cho các trường hợp sau không?
  - Tạo phiếu với `equipmentId` rỗng/blank?
  - Tạo phiếu với `description` < 10 ký tự hoặc > 500 ký tự?
  - Chuyển trạng thái nhảy cóc `OPEN → DONE`?
  - Chuyển trạng thái đảo ngược `IN_PROGRESS → OPEN`?
  - Tra cứu phiếu với UUID không tồn tại?
  - Gửi JSON body malformed (không parse được)?
- **Security Boundary Tests:**
  - Test truy cập endpoint với vai trò không được phép (ví dụ: `TECHNICIAN` gọi `POST /workorders`)?
  - Test truy cập endpoint không có authentication?
  - Test rate limiting (vượt ngưỡng 20 write req/min)?
- **Kim Tự Tháp Kiểm Thử:** Tỉ lệ Unit : Slice : Integration có hợp lý không (70:20:10)?
- **Test Fixtures:** `WorkOrderTestFixtures.java` có cung cấp đủ dữ liệu mẫu chuẩn hóa (Object Mother pattern)?

**Tiêu chuẩn PASS:** Mỗi endpoint API phải có ít nhất 1 happy path test + 2 negative tests; mỗi nhánh quyết định trong domain logic phải có test riêng; Security boundary tests phải cover toàn bộ ma trận RBAC.

---

### TIÊU CHÍ 5: DÙNG LOGGING KHÔNG ĐÚNG

**Câu hỏi kiểm toán:** Logging trong production code có tuân thủ chuẩn `docs/02-observability-and-logging.md` và best practices?

**Hướng dẫn kiểm tra:**
- **Cấm log PII (Personally Identifiable Information):** Kiểm tra tất cả lệnh `log.*()` trong 19 file production — có log nào ghi lại password, token, hoặc thông tin người dùng nhạy cảm không?
- **Log Level đúng ngữ cảnh:**
  - `log.info()` chỉ cho business events quan trọng (tạo phiếu, chuyển trạng thái)?
  - `log.warn()` cho validation failures, lỗi nghiệp vụ có thể khôi phục?
  - `log.error()` chỉ cho lỗi hệ thống không mong đợi (exception không bắt được)?
  - Có dùng `log.debug()` cho chi tiết kỹ thuật ở development?
- **Parameterized Logging:** Có dùng `log.info("msg {}", var)` thay vì `log.info("msg " + var)` (tránh string concatenation)?
- **Correlation ID Propagation:** `CorrelationIdFilter.java` có gán `traceId` và `correlationId` vào MDC không? Có dọn dẹp MDC trong `finally` không?
- **Không log toàn bộ Entity/DTO:** Có lệnh `log.info("entity: {}", workOrder)` nào dump toàn bộ object không?
- **Không log trong vòng lặp (loop):** Có lệnh log nào nằm trong vòng lặp gây flood log không?

**Tiêu chuẩn PASS:** Zero PII trong log; log level chính xác theo ngữ cảnh; parameterized logging 100%; MDC lifecycle đúng.

---

### TIÊU CHÍ 6: CHỨC NĂNG DƯ THỪA LÀM SAI THIẾT KẾ (Hàm Main)

**Câu hỏi kiểm toán:** Có class nào chứa phương thức `main()` ngoài `OmsApiApplication.java`? Có logic nghiệp vụ nào bị đặt sai vị trí trong kiến trúc 3 tầng?

**Hướng dẫn kiểm tra:**
- Tìm kiếm tất cả phương thức `public static void main(String[] args)` trong toàn bộ codebase. Chỉ được phép tồn tại trong `OmsApiApplication.java`.
- Kiểm tra `OmsApiApplication.java` — có logic nghiệp vụ nào (khởi tạo data, gọi service) trong class này không? Class này chỉ nên chứa `@SpringBootApplication` và `SpringApplication.run()`.
- Kiểm tra xem có `CommandLineRunner`, `ApplicationRunner`, hoặc `@PostConstruct` nào chạy logic nghiệp vụ khi khởi động không?
- **Kiến trúc 3 tầng:** Kiểm tra Controller có gọi trực tiếp Repository (bỏ qua Service) không? Service có trả về Entity thay vì DTO không?
- Kiểm tra thư mục `csv-vat-calculator/` — có phải là module dư thừa với hàm `main()` riêng?

**Tiêu chuẩn PASS:** Duy nhất 1 hàm `main()` trong `OmsApiApplication.java`; không có business logic trong Application class; tôn trọng tuyệt đối phân tầng Controller → Service → Repository.

---

### TIÊU CHÍ 7: THIẾT KẾ RƯỜM RÀ, KHÔNG CẦN THIẾT (Khai thác Framework & Language Features)

**Câu hỏi kiểm toán:** Mã nguồn có tận dụng tối đa các tính năng của Java 17 và Spring Boot 3.3 để giảm boilerplate, hay vẫn còn code rườm rà không cần thiết?

**Hướng dẫn kiểm tra:**
- **Java Records cho DTOs:** Các class `WorkOrderRequest`, `WorkOrderResponse`, `WorkOrderStatusRequest`, `PagedResponse` có dùng Java `record` không? Nếu dùng POJO truyền thống, có viết thủ công `equals()`, `hashCode()`, `toString()` không cần thiết không?
- **Lưu ý về Lombok:** Dự án cam kết **Zero Lombok** (xem `docs/00-coding-rules.md`). Nếu không dùng Lombok, thay thế bằng Records là thiết kế đúng. KHÔNG đánh giá "thiếu Lombok" là lỗi — thay vào đó kiểm tra Records có được dùng đúng chỗ.
- **Pattern Matching (Java 17):** Có dùng `instanceof` pattern matching thay vì cast thủ công không?
- **Text Blocks:** Có chuỗi nhiều dòng nào nên dùng text block `"""..."""` thay vì string concatenation?
- **Constructor Injection:** Spring beans có dùng constructor injection thay vì `@Autowired` trên field?
- **Compact Constructors (Records):** Records có sử dụng compact constructor cho validation thay vì viết canonical constructor đầy đủ?
- **Method Reference & Lambda:** Có chỗ nào dùng anonymous class thay vì lambda/method reference một cách không cần thiết?

**Tiêu chuẩn PASS:** DTOs dùng Java Records; Constructor Injection cho tất cả Spring beans; tận dụng Java 17 language features; không có boilerplate code không cần thiết.

---

### TIÊU CHÍ 8: ĐƯA THƯ MỤC VÀ FILE KHÔNG CẦN THIẾT LÊN GIT

**Câu hỏi kiểm toán:** Kho Git có chứa các file/thư mục không nên theo dõi (compiled artifacts, IDE configs, secrets, temp files)?

**Hướng dẫn kiểm tra:**
- **Kiểm tra `.gitignore`:** Có đầy đủ các pattern loại trừ cho:
  - `target/` (Maven build output)?
  - `*.jar`, `*.class` (compiled artifacts)?
  - `.idea/`, `.vscode/`, `*.iml` (IDE settings)?
  - `.env*`, `secrets/` (credentials)?
  - `*.log` (log files)?
- **Kiểm tra file thực tế trên Git:**
  - Thư mục `target/` có bị commit lên Git không?
  - File `.mvn/wrapper/maven-wrapper.jar` có bị track không? (nên exclude qua `.gitignore`)
  - Thư mục `csv-vat-calculator/` — có thuộc về dự án OMS không? Nếu không, đây là file dư thừa trên Git.
  - Thư mục `docs/archive/audit-logs/` — có cần thiết giữ trên Git hay nên archive riêng?
  - File `.vscode/` có bị track không?
- **Credentials & Secrets:** Kiểm tra `application.yml` — có hardcode password, API key, hoặc secret nào không?
- **Kích thước repository:** Có file nhị phân lớn (>1MB) nào bị commit không?

**Tiêu chuẩn PASS:** `.gitignore` bao quát đầy đủ; không có compiled artifacts, IDE-specific files, hoặc secrets trên Git; mọi file trên Git đều có lý do tồn tại rõ ràng.

---

### TIÊU CHÍ 9: TRÌNH BÀY MÃ NGUỒN THEO TIÊU CHUẨN CƠ BẢN (Format Code & Comments)

**Câu hỏi kiểm toán:** Mã nguồn có tuân thủ chuẩn trình bày Java, Javadoc, và comment conventions?

**Hướng dẫn kiểm tra:**
- **Indentation & Formatting:** Kiểm tra indent nhất quán (4 spaces cho Java). Có file nào trộn lẫn tabs và spaces không?
- **Naming Conventions:**
  - Classes: PascalCase (`WorkOrderController`, `WorkOrderService`)
  - Methods & variables: camelCase (`createWorkOrder`, `equipmentId`)
  - Constants: UPPER_SNAKE_CASE (`WRITE_CAPACITY`, `MAX_BUCKETS_THRESHOLD`)
  - Packages: lowercase (`com.gpc.oms.controller`)
- **Javadoc trên Public API:** Các public methods trong Controller và Service có Javadoc mô tả mục đích, tham số, và giá trị trả về không?
- **Comments chất lượng:** Có comment giải thích "WHY" (tại sao) thay vì "WHAT" (cái gì)? Có dead comments (code bị comment out) không?
- **Import Statements:** Có wildcard import `import java.util.*` nào không? Có unused imports không?
- **Line Length:** Có dòng nào vượt quá 120 ký tự gây khó đọc không?
- **Blank Lines:** Có nhóm logic được phân tách bằng blank lines hợp lý không?

**Tiêu chuẩn PASS:** Naming conventions nhất quán 100%; không có dead code/comments; imports gọn gàng; formatting đồng nhất.

---

### TIÊU CHÍ 10: THỰC HIỆN THIẾU YÊU CẦU (Testing)

**Câu hỏi kiểm toán:** Có yêu cầu kiểm thử nào trong đặc tả (`docs/drafts/draft-workorder-tests.md`) mà chưa được triển khai?

**Hướng dẫn kiểm tra:**
- Mở `docs/drafts/draft-workorder-tests.md` và đối chiếu danh sách test cases bắt buộc với 20 test files thực tế.
- **Ma trận coverage theo tầng:**
  - Domain Layer: `WorkOrderTest`, `WorkOrderStatusTest`, `PriorityTest` — có đủ không?
  - DTO Layer: `DtoMappingTest` — có test mapping Entity ↔ DTO chính xác không?
  - Service Layer: `WorkOrderServiceTest` — có mock Repository đúng cách không?
  - Controller Layer: `WorkOrderControllerTest` — có test HTTP status codes đầy đủ (200, 201, 400, 404, 422)?
  - Security Layer: Có test cho từng role trong ma trận RBAC không?
  - Integration Layer: `WorkOrderIntegrationTest` — có test end-to-end với database thật (H2) không?
- **Thiếu test quan trọng nào không?**
  - Test cho `@Profile("prod")` OAuth2 JWT flow?
  - Test cho concurrent access (race condition khi 2 request cùng PATCH status)?
  - Test cho database constraint violations?
  - Performance/load test?

**Tiêu chuẩn PASS:** 100% test cases trong đặc tả đều có triển khai; mỗi production class có ít nhất 1 test class tương ứng; JaCoCo báo cáo 100% Line & Branch Coverage.

---

### TIÊU CHÍ 11: LÀM SAI THIẾT KẾ (Tự Ý Thêm Tham Số Cho Hàm Mà Không Có Xác Nhận)

**Câu hỏi kiểm toán:** Có method signature nào trong mã nguồn khác biệt so với đặc tả thiết kế trong `docs/drafts/`?

**Hướng dẫn kiểm tra:**
- **Đối chiếu Controller method signatures** với `docs/02-api-spec.md`:
  - `createWorkOrder(@Valid @RequestBody WorkOrderRequest)` — đúng signature?
  - `getAllWorkOrders(status, page, size)` — có thêm parameter nào ngoài đặc tả?
  - `getWorkOrderById(@PathVariable UUID id)` — đúng kiểu dữ liệu?
  - `updateWorkOrderStatus(@PathVariable UUID id, @Valid @RequestBody WorkOrderStatusRequest)` — đúng?
- **Đối chiếu Service method signatures** với `docs/drafts/draft-workorder-service.md`:
  - Có method nào trong `WorkOrderService.java` nhận thêm tham số không có trong thiết kế?
  - Có method nào thay đổi kiểu trả về so với thiết kế (ví dụ: trả `Optional` thay vì throw exception)?
- **Đối chiếu DTO fields** với `docs/02-api-spec.md`:
  - `WorkOrderRequest` có field nào ngoài `equipmentId`, `description`, `priority`?
  - `WorkOrderResponse` có field nào ngoài đặc tả?
  - `WorkOrderStatusRequest` có field nào ngoài `status`?
- **Repository methods:** Có custom query method nào không có trong thiết kế ban đầu?

**Tiêu chuẩn PASS:** 100% method signatures khớp với đặc tả thiết kế; không có tham số hoặc field "tự thêm" mà không có tài liệu phê duyệt.

---

### TIÊU CHÍ 12: KHÔNG ĐỌC KỸ YÊU CẦU CỦA ĐỀ BÀI (Không Hiểu "Khách Hàng" Muốn Gì)

**Câu hỏi kiểm toán:** Mã nguồn có phản ánh đúng ý đồ nghiệp vụ của Product Owner hay có sự hiểu sai/bỏ sót yêu cầu gốc?

**Hướng dẫn kiểm tra:**
- Đọc lại **Yêu cầu Thô Ban Đầu** trong `docs/01-br-analysis-wo.md` (Section "Yêu cầu Thô Ban Đầu"):
  > _"Bộ phận Vận hành Lưới điện cần một tính năng mới trên ứng dụng để điều độ viên và thợ hiện trường báo cáo các sự kiện mất điện (Outage). Người dùng cần nhập mã thiết bị (`equipmentId`) bị lỗi, mô tả sự cố và đánh giá mức độ nghiêm trọng (`priority`). Hệ thống phải lưu lại toàn bộ vòng đời xử lý sự cố (từ lúc tạo đến lúc đóng phiếu). Dữ liệu này phải được đồng bộ liên tục về Nền tảng Dữ liệu trung tâm để phục vụ tính toán các chỉ số tin cậy cung cấp điện như SAIDI, SAIFI."_
- **Kiểm tra đối chiếu:**
  1. Có thể nhập `equipmentId` khi tạo phiếu? → Kiểm tra `WorkOrderRequest.java`
  2. Có thể nhập `description` sự cố? → Kiểm tra `WorkOrderRequest.java`
  3. Có thể đánh giá `priority`? → Kiểm tra `WorkOrderRequest.java`
  4. Hệ thống lưu toàn bộ vòng đời (`createdAt` → `resolvedAt`)? → Kiểm tra `WorkOrder.java`
  5. State Machine đúng luồng `OPEN → IN_PROGRESS → DONE`? → Kiểm tra `WorkOrderStatus.java`
  6. Khi chuyển sang `DONE`, `resolvedAt` có được gán `Instant.now()` không? → Kiểm tra `WorkOrder.advanceStatus()` hoặc `WorkOrderService`
  7. Phân quyền: Dispatcher tạo phiếu, Technician cập nhật trạng thái? → Kiểm tra `SecurityConfig.java` và `@PreAuthorize`
  8. SAIDI/SAIFI: Yêu cầu gốc đề cập tích hợp Debezium CDC — hệ thống có chuẩn bị sẵn sàng cho điều này không? (đây có thể là scope ngoài v1.0.0 — cần ghi nhận)

**Tiêu chuẩn PASS:** Tất cả 7 yêu cầu cốt lõi (mục 1-7) đều được hiện thực hóa đúng; mục 8 (CDC) được ghi nhận là roadmap nếu chưa triển khai.

---

### TIÊU CHÍ 13: TRIỂN KHAI SAI SO VỚI THIẾT KẾ (Nghiệp Vụ Tính Toán Các Bước Không Song Hành Code)

**Câu hỏi kiểm toán:** Logic nghiệp vụ trong mã nguồn có khớp chính xác với thuật toán xử lý (Step-by-step Logic) được mô tả trong đặc tả?

**Hướng dẫn kiểm tra:**
- **POST `/api/v1/workorders` — Step-by-step Logic** (từ `docs/02-api-spec.md` Section 1):
  1. Controller nhận `WorkOrderRequest` → kích hoạt Bean Validation `@Valid`?
  2. Controller ủy quyền cho `WorkOrderService.createWorkOrder(request)`?
  3. Service khởi tạo `WorkOrder` mới với `status = OPEN`, `createdAt = Instant.now()`?
  4. Service lưu qua `WorkOrderRepository.save(workOrder)`?
  5. Service trả về `WorkOrderResponse` (không trả Entity)?
  6. Controller trả HTTP 201 Created + Header `Location`?
  → Mở `WorkOrderController.createWorkOrder()` và `WorkOrderService.createWorkOrder()`, đối chiếu từng bước.

- **PATCH `/api/v1/workorders/{id}/status` — Step-by-step Logic** (từ `docs/02-api-spec.md` Section 4):
  1. Controller nhận `id` và `WorkOrderStatusRequest`?
  2. Service tìm WorkOrder theo `id`, throw 404 nếu không tìm thấy?
  3. Service gọi `workOrder.advanceStatus(newStatus)` — kiểm tra State Machine?
  4. `advanceStatus()` throw `IllegalStateException` nếu transition không hợp lệ?
  5. Nếu newStatus = `DONE`, gán `resolvedAt = Instant.now()`?
  6. Service lưu entity cập nhật?
  7. Controller trả HTTP 200 OK?
  → Mở source code thực tế, đối chiếu **từng dòng** với step-by-step logic.

- **GET `/api/v1/workorders` — Pagination Logic:**
  1. Có default values cho `page=0`, `size=10`?
  2. Có sắp xếp theo `createdAt DESC` (mới nhất trước)?
  3. Response có đúng structure `PagedResponse<WorkOrderResponse>`?

- **Xử lý lỗi — RFC 7807 Mapping:**
  → Đối chiếu từng loại exception trong `GlobalExceptionHandler.java` với bảng Error Catalog trong `docs/02-api-spec.md`.

**Tiêu chuẩn PASS:** 100% các step-by-step logic trong đặc tả đều có dòng code tương ứng; thứ tự thực thi trong code khớp với thứ tự trong đặc tả; error mapping 1:1 với RFC 7807 catalog.

---

## ĐỊNH DẠNG BÁO CÁO ĐẦU RA

Sau khi kiểm toán xong, tạo báo cáo dạng Markdown theo cấu trúc sau:

```markdown
# BÁO CÁO KIỂM TOÁN CHẤT LƯỢNG MÃ NGUỒN — 13 TIÊU CHÍ
## Dự án: oms-api-demo v1.0.0
## Thời điểm kiểm toán: [YYYY-MM-DD]

### BẢNG TỔNG KẾT

| # | Tiêu Chí | Kết Quả | Số Phát Hiện | Severity Cao Nhất |
|---|---|---|---|---|
| 1 | Code dư thừa | ✅ PASS / ❌ FAIL | N | 🔴/🟠/🟡 |
| 2 | Thuật toán không phù hợp | ... | ... | ... |
| ... | ... | ... | ... | ... |
| 13 | Triển khai sai so với thiết kế | ... | ... | ... |

### CHI TIẾT TỪNG TIÊU CHÍ

#### TIÊU CHÍ 1: Code dư thừa — ✅ PASS / ❌ FAIL

**Phát hiện 1.1:** [Mô tả ngắn gọn]
- **File:** `path/to/file.java` (dòng XX-YY)
- **Severity:** 🟠 MAJOR
- **Bằng chứng:** [Đoạn code trích dẫn]
- **Đối chiếu đặc tả:** [Tài liệu tham chiếu]
- **Khuyến nghị:** [Hành động sửa chữa cụ thể]

(... tiếp tục cho từng phát hiện ...)
```

---

## LƯU Ý QUAN TRỌNG

1. **KHÔNG** bỏ qua bất kỳ tiêu chí nào — phải đánh giá đầy đủ 13/13.
2. **KHÔNG** đánh giá cảm tính — mọi PASS/FAIL đều phải có bằng chứng trích dẫn.
3. Nếu một tiêu chí PASS hoàn toàn, vẫn phải ghi nhận "0 phát hiện" kèm bằng chứng kiểm tra.
4. Ưu tiên kiểm tra các file production trước (`src/main/`), sau đó đến test files (`src/test/`).
5. Kiểm tra chéo với `docs/drafts/` (12 bản thảo) để phát hiện drift giữa thiết kế và triển khai.
