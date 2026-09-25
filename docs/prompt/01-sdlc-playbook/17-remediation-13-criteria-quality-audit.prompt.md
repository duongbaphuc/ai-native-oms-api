<!--
Role: Principal Software Architect & Lead Remediation Engineer
Task: Khắc phục triệt để các phát hiện kiểm toán để đạt mức hoàn thiện 13/13 tiêu chí chất lượng
Context files:
  - docs/prompt/03-module-task-prompts/16-comprehensive-quality-audit-13-criteria.prompt.md
  - docs/01-br-analysis-wo.md (Yêu cầu nghiệp vụ gốc — Baseline SSOT)
  - docs/00-coding-rules.md (Chuẩn viết mã Java 17 / Spring Boot)
  - docs/00-api-rules.md (Chuẩn RESTful API, RFC 7807)
  - docs/00-internal-coding-standards.md (Chuẩn nội bộ: UTC Instant, cấm Lombok)
  - docs/00-security-rules.md (OWASP API, CWE-770 Rate Limiting)
Target files to fix:
  - Delete directory: csv-vat-calculator/
  - Build config: pom.xml (bổ sung Caffeine Cache nếu cần)
  - Rate limiting filter: src/main/java/com/gpc/oms/config/RateLimitingFilter.java
  - Service layer: src/main/java/com/gpc/oms/service/WorkOrderService.java
  - Domain layer: src/main/java/com/gpc/oms/domain/WorkOrder.java
  - Controller layer: src/main/java/com/gpc/oms/controller/WorkOrderController.java
  - Security config: src/main/java/com/gpc/oms/config/SecurityConfig.java
  - Exception handler: src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java
  - Drafts sync: docs/drafts/draft-observability-filters.md, docs/drafts/draft-workorder-service.md, docs/drafts/draft-workorder-domain.md, docs/drafts/draft-security-config.md
Verification:
  - ./mvnw test (117 tests PASS, 0 failure)
  - JaCoCo Line & Branch Coverage = 100%
  - Không còn dòng > 120 ký tự, không còn wildcard imports, zero redundant try-catch
-->

# PROMPT: KHẮC PHỤC TRIỆT ĐỂ PHÁT HIỆN KIỂM TOÁN — HOÀN THIỆN XUẤT SẮC 13/13 TIÊU CHÍ
## Dự án: Outage Work Order API (`oms-api-demo`) — Phiên bản v1.0.0

---

## 1. MỤC TIÊU & BỐI CẢNH

Dự án `ai-native-oms-api` vừa trải qua đợt kiểm toán toàn diện theo 13 tiêu chí chất lượng (ghi nhận tại Báo cáo kiểm toán từ prompt `16-comprehensive-quality-audit-13-criteria.prompt.md`). Mặc dù hệ thống đạt **117/117 test cases pass** và **100% Line & Branch Coverage trên JaCoCo**, nhưng vẫn còn một số điểm trừ ở các tiêu chí 1, 2, 7, và 9:
1. **Tiêu chí 1 (Code dư thừa):** Tồn tại thư mục ngoại lai `csv-vat-calculator/` trong workspace.
2. **Tiêu chí 2 (Thuật toán không phù hợp):** Lệnh `buckets.clear()` trong `RateLimitingFilter.java` khi bộ nhớ đệm vượt 10,000 mục gây rủi ro DoS / un-throttling cho các client vi phạm.
3. **Tiêu chí 7 (Thiết kế rườm rà):** Khối `try-catch` bắt rồi lập tức `re-throw` `IllegalStateException` tại dòng 59-63 trong `WorkOrderService.java` là boilerplate không cần thiết.
4. **Tiêu chí 9 (Chuẩn trình bày):** Còn tồn tại wildcard imports (`import jakarta.persistence.*`, `import org.springframework.web.bind.annotation.*`), thiếu Javadoc trên public API của `WorkOrderController` và `WorkOrderService`, và một số dòng code vượt quá độ dài chuẩn 120 ký tự.

Bạn đóng vai trò **Principal Software Architect & Lead Remediation Engineer**. Nhiệm vụ của bạn là thực hiện tái cấu trúc chính xác, triệt để theo 4 nhóm hành động bên dưới, đảm bảo sau khi hoàn tất, toàn bộ 13/13 tiêu chí đều đạt **✅ PASS 100%**, mã nguồn sạch đẹp, test suite chạy hoàn hảo và giữ vững nguyên tắc Zero-Draft-Drift.

---

## 2. HƯỚNG DẪN CHI TIẾT 4 HÀNH ĐỘNG KHẮC PHỤC

### HÀNH ĐỘNG 1: Dọn Dẹp Workspace & Loại Bỏ Thư Mục Dư Thừa (Khắc phục Tiêu chí 1)

1. **Xóa bỏ thư mục ngoại lai `csv-vat-calculator/`:**
   - Xóa hoàn toàn thư mục `csv-vat-calculator/` khỏi thư mục gốc dự án.
   - Thư mục này là một project Java độc lập khác, chứa `.classpath`, `.project`, `.settings/`, `bin/`, `target/`, file zip nhị phân, v.v., không thuộc dự án OMS API và không nằm trong `draft-file-mapping.md`.
2. **Kiểm tra trạng thái Git:**
   - Đảm bảo `git status` không hiển thị bất kỳ tệp dư thừa nào thuộc thư mục này.
3. **Lưu ý về Web Console `src/main/resources/static/index.html`:**
   - Giữ nguyên tệp này vì nó đóng vai trò Interactive Test Console phục vụ nghiệm thu trực quan và kiểm thử của Điều độ viên / Kỹ thuật viên (theo ADR-001 và SDLC Playbook Phase 6), nhưng cần đảm bảo tài liệu kiến trúc ghi nhận đúng vai trò là công cụ hỗ trợ kiểm thử.

---

### HÀNH ĐỘNG 2: Nâng Cấp Thuật Toán Rate Limiting sang Caffeine Cache LRU Eviction (Khắc phục Tiêu chí 2)

1. **Cập nhật `pom.xml`:**
   - Khai báo dependency `com.github.ben-manes.caffeine:caffeine` (phiên bản được Spring Boot 3.3 dependency management quản lý tự động, không cần ghi đè version hoặc dùng bản mới nhất tương thích):
     ```xml
     <!-- Caffeine High-Performance In-Memory Caching for Rate Limiting Eviction -->
     <dependency>
         <groupId>com.github.ben-manes.caffeine</groupId>
         <artifactId>caffeine</artifactId>
     </dependency>
     ```
2. **Tái cấu trúc `src/main/java/com/gpc/oms/config/RateLimitingFilter.java`:**
   - Thay thế `ConcurrentHashMap<String, Bucket> buckets` bằng `com.github.benmanes.caffeine.cache.Cache<String, Bucket>`:
     ```java
     private final com.github.benmanes.caffeine.cache.Cache<String, Bucket> buckets =
             com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                     .maximumSize(MAX_CACHE_ENTRIES)
                     .expireAfterAccess(Duration.ofMinutes(10))
                     .build();
     ```
   - Thay thế lệnh `buckets.size() > MAX_CACHE_ENTRIES` và lệnh nguy hiểm `buckets.clear()`:
     * Loại bỏ hoàn toàn khối `if (buckets.size() > MAX_CACHE_ENTRIES) { buckets.clear(); }`.
     * Caffeine sẽ tự động quản lý kích thước bằng thuật toán Window TinyLFU / LRU eviction, loại bỏ dần các bucket ít sử dụng nhất khi chạm mốc `MAX_CACHE_ENTRIES = 10_000` mà **KHÔNG xóa sạch** bucket của các IP đang bị phạt.
   - Thay thế `buckets.computeIfAbsent(cacheKey, key -> createNewBucket(isRead))` bằng:
     ```java
     Bucket bucket = buckets.get(cacheKey, key -> createNewBucket(isRead));
     ```
3. **Xác nhận `RateLimitingFilterTest.java`:**
   - Chạy test đảm bảo cơ chế Token Bucket (60 req/min cho Read, 20 req/min cho Write, HTTP 429 + `Retry-After`) tiếp tục hoạt động chính xác 100%.

---

### HÀNH ĐỘNG 3: Loại Bỏ Khối Try-Catch Dư Thừa trong Service (Khắc phục Tiêu chí 7)

1. **Tái cấu trúc `src/main/java/com/gpc/oms/service/WorkOrderService.java`:**
   - Tại phương thức `updateStatus(final UUID id, final WorkOrderStatusRequest req)` (dòng 59-63 hiện tại):
     ```java
     // TRƯỚC ĐÂY (RƯỜM RÀ):
     final WorkOrderStatus fromStatus = entity.getStatus();
     try {
         entity.advanceStatus(req.status());
     } catch (IllegalStateException ex) {
         throw ex; // Re-throw — GlobalExceptionHandler sẽ map thành 422
     }
     final WorkOrder saved = repo.save(entity);
     ```
   - Chuyển thành mã tinh gọn:
     ```java
     // SAU KHI TỐI ƯU (TINH GỌN):
     final WorkOrderStatus fromStatus = entity.getStatus();
     entity.advanceStatus(req.status());
     final WorkOrder saved = repo.save(entity);
     ```
   - **Giải thích:** `IllegalStateException` là unchecked exception (`RuntimeException`). Việc không dùng try-catch giúp code ngắn gọn, và exception vẫn được ném tự nhiên lên `GlobalExceptionHandler.handleIllegalStateTransition()` để ánh xạ thành HTTP 422 `urn:problem-type:invalid-state-transition`.

---

### HÀNH ĐỘNG 4: Chuẩn Hóa Code Formatting, Javadoc & Triệt Tiêu Dòng Dài (Khắc phục Tiêu chí 9)

1. **Triệt tiêu Wildcard Imports:**
   - Tại `src/main/java/com/gpc/oms/domain/WorkOrder.java`:
     - Thay `import jakarta.persistence.*;` bằng:
       ```java
       import jakarta.persistence.Column;
       import jakarta.persistence.Entity;
       import jakarta.persistence.EnumType;
       import jakarta.persistence.Enumerated;
       import jakarta.persistence.GeneratedValue;
       import jakarta.persistence.GenerationType;
       import jakarta.persistence.Id;
       import jakarta.persistence.Table;
       ```
   - Tại `src/main/java/com/gpc/oms/controller/WorkOrderController.java`:
     - Thay `import org.springframework.web.bind.annotation.*;` bằng:
       ```java
       import org.springframework.web.bind.annotation.GetMapping;
       import org.springframework.web.bind.annotation.PatchMapping;
       import org.springframework.web.bind.annotation.PathVariable;
       import org.springframework.web.bind.annotation.PostMapping;
       import org.springframework.web.bind.annotation.RequestBody;
       import org.springframework.web.bind.annotation.RequestMapping;
       import org.springframework.web.bind.annotation.RequestParam;
       import org.springframework.web.bind.annotation.RestController;
       ```
2. **Bổ sung Javadoc chuẩn hóa cho Public API Methods:**
   - **Trong `WorkOrderController.java`:** Viết Javadoc đầy đủ cho cả 4 phương thức:
     * `createWorkOrder`: Mô tả tiếp nhận tạo phiếu sự cố, phân quyền `DISPATCHER`, `TECHNICIAN`, `ADMIN`, mã 201 Created + header `Location`.
     * `getWorkOrders`: Mô tả tra cứu danh sách có phân trang và lọc theo trạng thái, tham số `pageable`, `status`, mã 200 OK.
     * `getWorkOrderById`: Mô tả tra cứu chi tiết theo UUID, mã 200 OK hoặc 404 Not Found.
     * `updateStatus`: Mô tả cập nhật trạng thái phiếu sự cố, phân quyền `TECHNICIAN`, `ADMIN`, mã 200 OK, 404 hoặc 422.
   - **Trong `WorkOrderService.java`:** Viết Javadoc đầy đủ cho cả 4 phương thức tương ứng, mô tả rõ các ngoại lệ có thể ném ra (`@throws ResourceNotFoundException`, `@throws IllegalStateException`), tương tác persistence và ghi nhận Micrometer metrics.
3. **Ngắt các dòng code vượt quá độ dài chuẩn 120 ký tự:**
   - Rà soát và ngắt dòng hợp lý trong:
     * `SecurityConfig.java`: Dòng signature `filterChain`, các dòng ghi chuỗi ProblemDetail JSON.
     * `RateLimitingFilter.java`: Chuỗi JSON template và các log parameters.
     * `GlobalExceptionHandler.java`: Các dòng chú thích và xử lý logic dài hơn 120 ký tự.
     * `WorkOrderService.java`: Dòng comment AI provenance ở đầu file.

---

### HÀNH ĐỘNG 5: Đồng Bộ Hóa Bản Thảo (Drafts Sync) & Kiểm Thử Nghiệm Thu Tuyệt Đối

1. **Đồng bộ hóa các file Drafts trong `docs/drafts/`:**
   - `docs/drafts/draft-observability-filters.md`: Cập nhật cấu trúc cache Caffeine thay cho `ConcurrentHashMap.clear()`.
   - `docs/drafts/draft-workorder-service.md`: Bỏ khối try-catch thừa trong mã mẫu `updateStatus`.
   - `docs/drafts/draft-workorder-domain.md` và `draft-workorder-create.md`: Cập nhật import tường minh thay cho wildcard.
2. **Biên dịch và Chạy kiểm thử tự động:**
   - Thực thi lệnh: `./mvnw clean test`
   - Điều kiện nghiệm thu bắt buộc:
     * `Tests run: 117, Failures: 0, Errors: 0, Skipped: 0`
     * JaCoCo Line Coverage: **100%**
     * JaCoCo Branch Coverage: **100%**
     * 0 compilation warnings về deprecation hay raw types.
3. **Rà soát lại 13 tiêu chí:**
   - Tự động chạy lại ma trận kiểm toán 13 tiêu chí và xác nhận **13/13 PASS**.

---

## 3. ĐỊNH DẠNG BÁO CÁO KẾT QUẢ SAU REMEDIATION

Sau khi hoàn tất, hãy xuất bản bảng tổng kết kết quả theo mẫu sau:

```markdown
# BÁO CÁO KẾT QUẢ KHẮC PHỤC PHÁT HIỆN KIỂM TOÁN 13 TIÊU CHÍ
## Dự án: Outage Work Order API (`oms-api-demo`) — Trạng Thái Sau Remediation

### 1. BẢNG TIÊU CHÍ ĐÃ ĐƯỢC CHUẨN HÓA

| # | Tiêu Chí | Trạng Thái Cũ | Trạng Thái Mới | Hành Động Đã Thực Hiện |
|---|---|:---:|:---:|---|
| 1 | Code dư thừa | ❌ FAIL | ✅ PASS | Đã xóa bỏ hoàn toàn thư mục csv-vat-calculator/ |
| 2 | Thuật toán không phù hợp | ❌ FAIL | ✅ PASS | Đã nâng cấp RateLimitingFilter sang Caffeine LRU eviction |
| 3 | Kiểu dữ liệu không phù hợp | ✅ PASS | ✅ PASS | Duy trì UTC Instant, Java Records, Primitives |
| 4 | Test case không đủ tin cậy | ✅ PASS | ✅ PASS | 117/117 test cases vượt qua hoàn hảo |
| 5 | Dùng logging không đúng | ✅ PASS | ✅ PASS | 100% parameterized, zero PII, MDC chuẩn |
| 6 | Chức năng dư thừa (Hàm Main)| ✅ PASS | ✅ PASS | 1 hàm main, 3-tier architecture nguyên vẹn |
| 7 | Thiết kế rườm rà | ❌ FAIL | ✅ PASS | Loại bỏ khối try-catch redundant trong WorkOrderService |
| 8 | File không cần thiết lên Git | ✅ PASS | ✅ PASS | Git index sạch, .gitignore bao quát đầy đủ |
| 9 | Trình bày mã nguồn & Javadoc| ❌ FAIL | ✅ PASS | Xóa wildcard imports, bổ sung Javadoc, ngắt dòng <120 |
| 10| Thiếu yêu cầu testing | ✅ PASS | ✅ PASS | JaCoCo 100% Line & Branch Coverage |
| 11| Làm sai thiết kế | ✅ PASS | ✅ PASS | 100% signatures & fields khớp đặc tả |
| 12| Không đọc kỹ yêu cầu PO | ✅ PASS | ✅ PASS | Đạt 8/8 yêu cầu nghiệp vụ cốt lõi |
| 13| Triển khai sai thiết kế | ✅ PASS | ✅ PASS | Step-by-step logic & RFC 7807 khớp 1:1 |

### 2. BẰNG CHỨNG THỰC THI (VERIFICATION EVIDENCE)
- Kết quả lệnh `./mvnw clean test`: [Số lượng test, thời gian]
- Báo cáo JaCoCo: [Line Coverage %, Branch Coverage %]
- Danh sách các file đã chỉnh sửa và diff code chính.
```
