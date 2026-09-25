# Prompt Giai Đoạn 04B: Đồng Bộ & Sinh Toàn Diện Các Bản Thảo Kỹ Thuật (Comprehensive Implementation Drafts & Blueprints Generation)

```markdown
# ROLE:
Bạn là một "Principal Technical Documentation Architect" kiêm "Lead AI-Native Context Engineer" với hơn 15 năm kinh nghiệm về Spec-Driven Development, Context Engineering cho Large Language Models (GitHub Copilot / Claude / GPT), và Thiết kế Kiến trúc Mã Nguồn Java 17 / Spring Boot 3.3. Mục tiêu tối thượng của bạn là rà soát toàn bộ các tệp mã nguồn hiện hữu trong `src/main/java/` (19 files) và `src/test/java/` (20 files), sau đó sinh đồng bộ 100% các tệp bản thảo Markdown (Implementation Drafts / Blueprints) trong `docs/drafts/` theo đúng khuôn mẫu kỹ thuật chuẩn của dự án. Mọi file Java trong codebase bắt buộc phải có một bản thảo thiết kế làm Nguồn Chân Lý Duy Nhất (Single Source of Truth) để AI Agent có thể tái sinh mã nguồn 1:1 mà không gặp bất kỳ hiện tượng ảo giác nào (Zero Context Hallucination).

---

# CONTEXT & NGUỒN DỮ LIỆU ĐỐI CHIẾU:
1. **Các file quy chuẩn kỹ thuật:**
   - [`docs/00-coding-rules.md`](docs/00-coding-rules.md): Quy chuẩn Oracle Core, Joshua Bloch, Constructor Injection, cấm Lombok, modifier `final`.
   - [`docs/02-security-auth-spec.md`](docs/02-security-auth-spec.md): Đặc tả Dual SecurityFilterChain, OAuth2 JWT Resource Server, Bucket4j Rate Limiting.
   - [`docs/02-observability-and-logging.md`](docs/02-observability-and-logging.md): Đặc tả MDC Logging, Correlation ID, Micrometer Prometheus.
   - [`docs/08-SYSTEM_HANDOVER.md`](docs/08-SYSTEM_HANDOVER.md): Hồ sơ bàn giao kỹ thuật, danh mục 39 tệp mã nguồn và 117 bài kiểm thử.
2. **Hiện trạng mã nguồn thực tế:**
   - `src/main/java/com/gpc/oms/` (19 files: Domain, DTO, Service, Controller, Exception, Config).
   - `src/test/java/com/gpc/oms/` (20 files: Unit, Slice, Integration, Security, Fixtures).
3. **Các file draft hiện hành trong `docs/drafts/`:**
   - `draft-dtos.md`, `draft-workorder-domain.md`, `draft-workorder-service.md`, `draft-workorder-create.md`, `draft-workorder-get.md`, `draft-workorder-patch.md`, `draft-global-exception-handler.md`, `draft-workorder-tests.md`, `draft-file-mapping.md`.

---

# TASK:
Thực hiện sinh mới và cập nhật đồng bộ hệ thống tài liệu Draft trong thư mục `docs/drafts/` theo 4 hạng mục kỹ thuật cốt lõi:

### Hạng Mục 1: Khởi Tạo `docs/drafts/draft-security-config.md`
Soạn thảo bản thảo kiến trúc bảo mật phục vụ sinh mã cho:
1. `src/main/java/com/gpc/oms/config/SecurityConfig.java`:
   - Đặc tả Dual `SecurityFilterChain`: `@Order(1)` `h2ConsoleChain` (`@Profile("!prod")`) và `@Order(2)` `filterChain`.
   - Cấu hình phân định đa môi trường: HTTP Basic Auth trên `!prod` vs OAuth2 Resource Server JWT trên `prod`.
   - Phân quyền RBAC cho Actuator endpoints: `/actuator/health` & `/actuator/info` công khai (`permitAll()`), `/actuator/prometheus` bảo vệ bởi `hasRole('ADMIN')`.
   - Tích hợp `CustomAuthenticationEntryPoint` trả về RFC 7807 `urn:problem-type:unauthorized` (HTTP 401).
2. `src/main/java/com/gpc/oms/config/JwtRoleConverter.java`:
   - Thuật toán trích xuất và chuẩn hóa role từ JWT claims (`realm_access.roles`, `resource_access.*.roles`, và `roles`), tự động gắn tiền tố `ROLE_`.
3. Bản phác thảo kiểm thử bảo mật (Test Sketches):
   - `H2ConsoleSecurityTest.java` (`H2ConsoleDevAccessTest` & `H2ConsoleProdAccessTest`).
   - `OAuth2JwtSecurityIntegrationTest.java` (Mock JWT decoder & RBAC checks).
   - `ActuatorSecurityTest.java` (Health probe vs Prometheus authorization).
   - `JwtRoleConverterTest.java` (Claims parsing & role mapping).

### Hạng Mục 2: Khởi Tạo `docs/drafts/draft-observability-filters.md`
Soạn thảo bản thảo các bộ lọc mạng và giám sát phân tán phục vụ sinh mã cho:
1. `src/main/java/com/gpc/oms/config/CorrelationIdFilter.java`:
   - Kế thừa `OncePerRequestFilter`. Thuật toán trích xuất header `X-Correlation-Id` hoặc tự sinh UUID v4.
   - Nạp vào SLF4J MDC `traceId`, ghi response header `X-Correlation-Id`, và khối `finally { MDC.clear(); }` chống rò rỉ ThreadLocal.
2. `src/main/java/com/gpc/oms/config/RateLimitingFilter.java`:
   - Kế thừa `OncePerRequestFilter`. Tích hợp Bucket4j Token Bucket với `ConcurrentHashMap` theo client IP.
   - Phân tách chính sách: Ghi (`POST`, `PATCH`) 10 tokens/phút; Đọc (`GET`) 60 tokens/phút.
   - Hàm `shouldNotFilter` loại trừ `/actuator/**`, `/`, `/index.html`, `/favicon.ico`, `/h2-console/**`.
   - Phản hồi HTTP 429 RFC 7807 `urn:problem-type:rate-limit-exceeded`.
3. Bản phác thảo kiểm thử bộ lọc (Test Sketches):
   - `CorrelationIdFilterTest.java` (HeaderExtractionTests, LifecycleAndExceptionTests).
   - `RateLimitingFilterTest.java` (ReadPolicyTests, WritePolicyTests, ShouldNotFilterTests, ClientIpResolutionTests).

### Hạng Mục 3: Khởi Tạo `docs/drafts/draft-shared-components.md`
Soạn thảo bản thảo các thành phần tối ưu hóa cú pháp & dùng chung phục vụ sinh mã cho:
1. `src/main/java/com/gpc/oms/exception/ProblemTypes.java`:
   - Lớp tiện ích `final` với private constructor, tập trung hóa 8 hằng số `java.net.URI` RFC 7807 (`VALIDATION_ERROR`, `MALFORMED_JSON`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `INVALID_STATE_TRANSITION`, `RATE_LIMIT_EXCEEDED`, `INTERNAL_ERROR`).
2. `src/main/java/com/gpc/oms/config/StringToWorkOrderStatusConverter.java`:
   - Cài đặt `Converter<String, WorkOrderStatus>`. Caching mảng `WorkOrderStatus.values()` tĩnh để tối ưu GC Heap.
   - Chuẩn hóa chuỗi đầu vào (trim, uppercase) và ném `IllegalArgumentException` khi giá trị không hợp lệ.
3. `src/test/java/com/gpc/oms/testutil/WorkOrderTestFixtures.java`:
   - Áp dụng Object Mother Pattern cung cấp các static factory methods tạo đối tượng kiểm thử mẫu: `createDefaultWorkOrder()`, `createDefaultRequest()`, `createDoneWorkOrder()`, `createDefaultResponse()`.
4. Bản phác thảo kiểm thử:
   - `ProblemTypesTest.java`, `ResourceNotFoundExceptionTest.java`, `StringToWorkOrderStatusConverterTest.java`.

### Hạng Mục 4: Cập Nhật `docs/drafts/draft-workorder-tests.md` & `draft-file-mapping.md`
1. **Cập nhật `docs/drafts/draft-workorder-tests.md`:**
   - Mở rộng ma trận chấp nhận (Acceptance Matrix) bao quát toàn bộ 20 lớp kiểm thử của Testing Pyramid (Unit, Slice, DataJpa, Security, Integration).
   - Bổ sung phác thảo test cho: `WorkOrderStatusTest.java` (ma trận 3x3), `PriorityTest.java`, `DtoMappingTest.java`, `WorkOrderServiceTest.java` (Mockito + Fixtures), `WorkOrderRepositoryTest.java` (`@DataJpaTest`), `GlobalExceptionHandlerUnitTest.java` (7 RFC 7807 handlers), và `WorkOrderIntegrationTest.java` (3 nested test classes).
2. **Cập nhật toàn diện `docs/drafts/draft-file-mapping.md`:**
   - Mở rộng bảng ánh xạ tệp từ 14 files ban đầu lên đầy đủ **39 files Java thực tế**:
     * Bảng Production Code: Đủ 19 files (Domain, DTO, Service, Controller, Exception, Config).
     * Bảng Test Code: Đủ 20 files (Unit, Slice, Integration, Security, Fixtures).
   - Cập nhật sơ đồ Package Structure (Visualization) và thứ tự dependencies implementation.

---

# CONSTRAINTS & FORMAT TIÊU CHUẨN:
1. **Header Tiêu Chuẩn Cho Mọi Tệp Draft:** Bắt buộc bắt đầu bằng khối HTML comment theo đúng phong cách cũ của dự án:
   ```markdown
   <!--
   Role: Senior Engineer. Task: <Mô tả tác vụ cụ thể>
   Context files: <Danh sách tệp quy chuẩn liên quan trong docs/>
   Constraints: Pure Java 17, Zero Lombok, RFC 7807, Spring Security 6.3.4, Constructor Injection.
   Architecture: Clean Architecture 3-Tier, Defense-in-Depth, Stateless REST.
   DRAFT ONLY — scoring target, never wired into app.
   -->
   ```
2. **Cấu Trúc Từng Tệp Draft:**
   - Mục 1: Bảng Schema / Target File Metadata (Tên file, Package, Path, Action, Mục đích).
   - Mục 2: Thuật toán từng bước (Step-by-step Logic / Pseudo-code).
   - Mục 3: Cảnh báo quan trọng (`> [!IMPORTANT]`, `> [!NOTE]`, `> [!WARNING]`).
   - Mục 4: Khối mã nguồn mẫu (Code Sketch) có comment `// AI Provenance: generated from docs/...`.
3. **Pure Java 17 & Joshua Bloch Alignment:**
   - 100% Request/Response DTOs là `record`.
   - Tuyệt đối KHÔNG sử dụng Lombok (`@Data`, `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`).
   - Sử dụng từ khóa `final` cho 100% tham số và biến cục bộ.

---

# DONE WHEN:
1. Tệp [`docs/drafts/draft-file-mapping.md`](docs/drafts/draft-file-mapping.md) được cập nhật đủ 39 tệp mã nguồn Java.
2. Các tệp mới [`draft-security-config.md`](docs/drafts/draft-security-config.md), [`draft-observability-filters.md`](docs/drafts/draft-observability-filters.md), [`draft-shared-components.md`](docs/drafts/draft-shared-components.md) được khởi tạo đầy đủ trong `docs/drafts/`.
3. Tệp [`docs/drafts/draft-workorder-tests.md`](docs/drafts/draft-workorder-tests.md) bao quát toàn bộ 20 lớp kiểm thử của kim tự tháp 117 tests.
4. Tỷ lệ ánh xạ giữa file code Java thực tế trong `src/` và file draft đạt **100% Zero-Draft-Drift**.
```
