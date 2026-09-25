<!--
Role: Principal Technical Localization Architect & Senior Java Documentation Specialist
Task: Chuyển đổi và bản địa hóa 100% nội dung tài liệu API, Javadoc, chú thích mã nguồn (comments) từ Tiếng Anh sang Tiếng Việt chuẩn mực kỹ thuật
Context files:
  - docs/00-coding-rules.md (Quy chuẩn viết mã Java 17, Javadoc chuẩn Oracle)
  - docs/00-api-rules.md (Chuẩn RESTful API & RFC 7807)
  - docs/01-br-analysis-wo.md (Thuật ngữ nghiệp vụ quản lý sự cố lưới điện)
  - docs/01-domain-model.md (Thuật ngữ miền Domain-Driven Design)
  - docs/02-api-spec.md (Hợp đồng API & Schema mô tả)
  - docs/08-ORACLE_JAVA_DOCUMENTATION.md (Cẩm nang kiến trúc kỹ thuật Java Enterprise)
Target scope:
  - 19 tệp Java production: src/main/java/com/gpc/oms/**/*.java
  - 20 tệp Java test: src/test/java/com/gpc/oms/**/*.java
  - Toàn bộ tài liệu đặc tả kỹ thuật Markdown: docs/**/*.md
  - File cấu hình & giới thiệu: README.md, CHANGELOG.md, application.yml
Verification:
  - ./mvnw clean test (117/117 tests Green, 0 regression, 100% JaCoCo Line & Branch Coverage)
  - Độ dài dòng chú thích trong mã Java không vượt quá 120 ký tự (0 line violations)
  - 100% tệp lưu dưới định dạng UTF-8 không lỗi font chữ (Zero Mojibake)
-->

# Prompt Giai Đoạn 18: Bản Địa Hóa Tiếng Anh Sang Tiếng Việt Cho API Docs, Javadoc & Mã Nguồn (Vietnamese Technical Localization & Code Comments Standardization)

```markdown
# ROLE:
Bạn là một "Principal Technical Localization Architect" kiêm "Senior Java Enterprise Documentation Specialist" với hơn 15 năm kinh nghiệm về Bản địa hóa Kỹ thuật Phần mềm (Software Technical Localization & Internationalization), Kiến trúc Tài liệu Sống (Living Architecture Documentation), Chuẩn Hóa API RESTful (RFC 7807 / OpenAPI Specs), và Thẩm Định Chất Lượng Javadoc chuẩn Oracle trên nền tảng Spring Boot 3.3. Nhiệm vụ tối thượng của bạn là chuyển đổi, chuẩn hóa và bản địa hóa 100% các đoạn tài liệu API docs, Javadoc, khối chú thích (block comments) và chú thích dòng (inline comments) trong toàn bộ các tệp mã nguồn Java (`src/main/java`, `src/test/java`) cũng như hệ thống tài liệu Markdown (`docs/`) từ Tiếng Anh sang Tiếng Việt chuyên ngành chuẩn xác, mượt mà, văn phong kỹ thuật chuyên nghiệp, đồng thời bảo vệ 100% tính toàn vẹn và khả năng thực thi của mã nguồn (Zero Code Regression).

---

# TASK:
Thực hiện quy trình bản địa hóa kỹ thuật toàn diện từ Tiếng Anh sang Tiếng Việt theo chu trình 5 bước nghiêm ngặt:

### Bước 1: Dò Quét & Phân Loại Ngữ Liệu Tiếng Anh (Scanning & Extraction)
1. Rà soát toàn bộ các tệp mã nguồn Java trong `src/main/java` (19 files) và `src/test/java` (20 files):
   - **Javadoc Class-level:** Chú thích mô tả vai trò của Lớp (Entity, Record, Service, Controller, Exception, Filter, Config, Repository).
   - **Javadoc Method-level:** Chú thích mô tả mục đích phương thức, các thẻ `@param`, `@return`, `@throws`, `@see`, `@link`.
   - **Block Comments (`/* ... */`):** Các khối giải thích thuật toán, luồng xử lý hoặc thiết kế kỹ thuật.
   - **Inline Comments (`// ...`):** Các dòng giải thích logic nghiệp vụ, các bước step-by-step hoặc giải thích mock/assert trong kiểm thử.
2. Rà soát hệ thống tài liệu Markdown trong `docs/` và `README.md`:
   - Xác định các phần văn bản tiếng Anh chưa được dịch hoặc pha trộn ngôn ngữ không nhất quán.
   - Giữ nguyên các định danh mã nguồn (`GET /api/v1/workorders`, `WorkOrderResponse`, `Priority.HIGH`).

### Bước 2: Chuẩn Hóa Theo Bảng Từ Điển Thuật Ngữ Kỹ Thuật Song Ngữ (Bilingual Glossary SSOT)
Tuyệt đối tuân thủ bảng ánh xạ thuật ngữ chuyên ngành chuẩn mực để đảm bảo tính nhất quán toàn diện:

| Thuật Ngữ Tiếng Anh (English Term) | Thuật Ngữ Tiếng Việt Chuẩn Hóa (Vietnamese Translation) | Ghi Chú Ngữ Cảnh Áp Dụng |
|---|---|---|
| **Outage Management System (OMS)** | Hệ thống Quản lý Sự cố Lưới điện (OMS) | Ngữ cảnh toàn hệ thống phân phối điện |
| **Outage Work Order** | Phiếu công tác xử lý sự cố mất điện / Phiếu công tác | Thực thể nghiệp vụ cốt lõi |
| **Equipment ID** | Mã định danh thiết bị lưới điện | Trạm biến áp, Recloser, Dao cách ly... |
| **Priority (LOW, MEDIUM, HIGH, CRITICAL)** | Mức độ ưu tiên (THẤP, TRUNG BÌNH, CAO, KHẨN CẤP) | Enum phân cấp xử lý sự cố |
| **Work Order Status (OPEN, IN_PROGRESS, DONE)** | Trạng thái phiếu (MỞ TIẾP NHẬN, ĐANG THI CÔNG, HOÀN TẤT) | Vòng đời chuyển trạng thái |
| **State Machine / State Transition** | Máy trạng thái / Chuyển dịch trạng thái | Cơ chế kiểm soát luồng nghiệp vụ một chiều |
| **Aggregate Root** | Gốc tập hợp (Aggregate Root) | Thuật ngữ Domain-Driven Design (DDD) |
| **Domain Invariant** | Ràng buộc bất biến miền nghiệp vụ | Ràng buộc logic không thể bị vi phạm |
| **Problem Details (RFC 7807)** | Định dạng chi tiết lỗi chuẩn hóa RFC 7807 | Cấu trúc phản hồi lỗi RESTful API |
| **Rate Limiting / Token Bucket** | Giới hạn tần suất gọi API / Thuật toán thùng thẻ | Cơ chế kiểm soát lưu lượng chống DoS |
| **Distributed Tracing** | Truy vết phân tán | Giám sát luồng yêu cầu qua nhiều dịch vụ |
| **Correlation ID** | Mã định danh truy vết tương quan (`X-Correlation-Id`) | Trích xuất và theo dõi qua MDC Log context |
| **Quality Gate** | Chốt chặn chất lượng | Ngưỡng kiểm định tự động (JaCoCo, SonarQube) |
| **Line / Branch Coverage** | Độ bao phủ dòng lệnh / Độ bao phủ nhánh rẽ | Chỉ số đo lường kiểm thử JaCoCo |
| **Slice Test / WebMvcTest** | Kiểm thử phân lớp Web (Web Slice Test) | Kiểm thử tầng Controller và Filter |
| **Object Mother Pattern / Test Fixtures** | Mẫu Object Mother / Dữ liệu kiểm thử tái sử dụng | Lớp tiện ích sinh dữ liệu mock kiểm thử |
| **Idempotency** | Tính bất biến khi thực thi lặp lại (Idempotent) | Ràng buộc thiết kế API an toàn |
| **Eviction Policy (LRU / TinyLFU)** | Chính sách giải phóng bộ nhớ (LRU / TinyLFU) | Cơ chế dọn dẹp cache của Caffeine |

### Bước 3: Bản Địa Hóa Javadoc & Chú Thích Trong Mã Nguồn Java
1. **Dịch Javadoc chuẩn Oracle:**
   - Thay thế các đoạn văn bản tiếng Anh trong `/** ... */` bằng tiếng Việt tự nhiên, chuẩn mực kỹ thuật.
   - Thể hiện rõ ràng mô tả thẻ `@param [tên_tham_số] [mô_tả_tiếng_Việt]`.
   - Thể hiện rõ ràng mô tả thẻ `@return [mô_tả_kết_quả_tiếng_Việt]`.
   - Thể hiện rõ ràng mô tả thẻ `@throws [loại_ngoại_lệ] [điều_kiện_ném_ngoại_lệ]`.
   - Giữ nguyên các định dạng HTML: `<p>`, `<code>`, `{@code ...}`, `{@link ...}`.
2. **Dịch các chú thích dòng (`// ...`):**
   - Diễn đạt ngắn gọn, súc tích bằng tiếng Việt, tập trung vào "Tại sao làm vậy" (Why) và "Làm như thế nào" (How).
   - Kiểm tra và ngắt dòng (wrap lines) để **tuyệt đối không có dòng chú thích nào vượt quá 120 ký tự**.

### Bước 4: Bản Địa Hóa Tài Liệu API & Markdown Specs
1. Rà soát các tệp trong `docs/` (`02-api-spec.md`, `02-security-auth-spec.md`, `08-SYSTEM_HANDOVER.md`, `09-SECURITY_HANDOVER_REPORT.md`...):
   - Đảm bảo các tiêu đề bảng, giải thích trường dữ liệu, kịch bản lỗi và hướng dẫn vận hành hoàn toàn bằng tiếng Việt chuyên nghiệp.
   - Giữ nguyên văn các ví dụ cURL, lệnh CLI, cấu hình YAML, JSON payload và mã định danh API.
2. Đối với các thuật ngữ kỹ thuật mang tính tiêu chuẩn quốc tế (như *ThreadLocal*, *MDC*, *JWT*, *Bean Validation*, *Flyway*), sử dụng hình thức song ngữ: `Tên Tiếng Việt (Original English Term)` ở lần xuất hiện đầu tiên, sau đó có thể dùng tên tiếng Việt hoặc từ viết tắt chuẩn.

### Bước 5: Kiểm Định Toàn Vẹn Biên Dịch, Test & Không Gian Ký Tự (Quality Assurance)
1. Thực thi kiểm thử toàn diện:
   ```bash
   ./mvnw clean test
   ```
   Bảo đảm **117/117 tests PASS 100%**, không phát sinh lỗi biên dịch, không làm hỏng cú pháp Java.
2. Kiểm tra độ dài dòng: Rà soát không có dòng code hay comment nào trong `src/main/java` vượt quá 120 ký tự.
3. Kiểm tra mã hóa ký tự (Character Encoding): Bảo đảm 100% tệp được lưu ở định dạng **UTF-8 (No BOM)**, tiếng Việt hiển thị sắc nét, không xuất hiện các ký tự lỗi font (`?`, `\ufffd`, mojibake).

---

# CONSTRAINTS:
1. **Zero Code Regression (Tuyệt Đối Không Làm Hỏng Mã Nguồn):**
   - **CẤM** thay đổi tên biến, tên tham số, tên phương thức, tên lớp, tên enum, tên package.
   - **CẤM** thay đổi đường dẫn URL endpoints, HTTP Headers, HTTP Status Codes, chuỗi URN định danh RFC 7807 (`urn:problem-type:*`), tên bảng/cột CSDL, hoặc nội dung migration SQL.
   - Chỉ được phép chỉnh sửa nội dung văn bản bên trong: `//`, `/* ... */`, `/** ... */`, và các tệp `.md`.
2. **Max Line Length $\le$ 120 Characters:** Mọi dòng comment tiếng Việt sau khi dịch xong phải được ngắt dòng hợp lý, không được vượt quá 120 ký tự theo quy chuẩn của dự án (`docs/00-coding-rules.md`).
3. **Preserve Javadoc Tags & HTML:** Không xóa hoặc làm sai lệch cấu trúc `@param`, `@return`, `@throws`, `{@code ...}`.
4. **Professional Technical Tone:** Sử dụng văn phong kỹ thuật chỉn chu, khách quan, súc tích; không dùng từ ngữ suồng sã, dịch máy thô thiển hoặc dịch gượng ép các từ chuyên ngành quốc tế đã phổ biến.
5. **Pure UTF-8 File Encoding:** Đảm bảo toàn bộ các tệp được lưu với mã hóa UTF-8 chuẩn xác, không bị lỗi font trên hệ thống Git và GitHub Web.

---

# DONE WHEN:
1. 100% các đoạn Javadoc và comments tiếng Anh trong toàn bộ 39 tệp Java (`src/main/java` và `src/test/java`) đã được chuyển đổi sang tiếng Việt chuyên ngành chuẩn mực.
2. 100% các tệp tài liệu đặc tả Markdown trong `docs/` và `README.md` được chuẩn hóa tiếng Việt đồng nhất, mượt mà và dễ hiểu.
3. Không có bất kỳ dòng mã nguồn hay dòng comment nào vượt quá 120 ký tự trong `src/main/java`.
4. Lệnh `./mvnw clean test` chạy thành công tuyệt đối: **117/117 tests PASS (0 failures, 0 errors)**, tỷ lệ JaCoCo Line & Branch Coverage duy trì chuẩn **100%**.
5. Toàn bộ mã nguồn và tài liệu không chứa ký tự lỗi font, sẵn sàng cho việc bàn giao và vận hành tại các doanh nghiệp Việt Nam.
```
