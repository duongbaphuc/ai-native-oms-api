# Prompt: Triển Khai SEC-05 - Tích Hợp OAuth2 Resource Server Xác Thực JWT Tập Trung Đạt Chuẩn Oracle Java (Issue #33)

> **Mã tính năng / Issue:** `[FEATURE] SEC-05: Tích hợp OAuth2 Resource Server xác thực JWT tập trung trên môi trường Production` ([#33](https://github.com/duongbaphuc/ai-native-oms-api/issues/33))  
> **Phân loại:** Enterprise Security, Identity Federation & RBAC Enforcement Task Prompt  
> **Nền tảng công nghệ:** OpenJDK / Oracle Java 17 LTS | Spring Boot 3.3.4 | Spring Security 6.x | OAuth2 Resource Server | Nimbus JWT  
> **Quy chuẩn đối chiếu:**  
> - `docs/02-security-auth-spec.md §2-3` (JWT Claims Schema & Ma trận phân quyền RBAC)  
> - `docs/09-SECURITY_HANDOVER_REPORT.md §3` (Lỗ hổng SEC-05, CWE-798 Hardcoded / Local Credentials in Production Gap)  
> - `docs/00-security-rules.md §1, §3` (Quy chuẩn An ninh Tầng Biên giới Mạng)  
> - `docs/08-ORACLE_JAVA_DOCUMENTATION.md` & `docs/00-coding-rules.md` (Oracle Java Coding Standards, JEP 106 Javadoc Tags)  
> - `docs/00-internal-coding-standards.md` & `pom.xml` (JaCoCo Quality Gate: 100% Line & Branch Coverage)

---

```markdown
# ROLE:
Bạn là một "Oracle Certified Master Java Enterprise Architect" kiêm "Principal Identity & Access Management (IAM) Security Specialist". Bạn sở hữu chuyên môn sâu sắc về chuẩn lập trình Oracle Java 17 LTS, đặc tả Javadoc (JEP 106), kiến trúc OAuth2 / OpenID Connect (OIDC), cơ chế giải mã và chuyển đổi JWT (JSON Web Token), và thiết kế phân quyền RBAC phân tán không trạng thái (Stateless Zero-Trust Security).

---

# CONTEXT:
Theo Hồ sơ Bàn giao An ninh (`docs/09-SECURITY_HANDOVER_REPORT.md §3`, mã phát hiện SEC-05 / CWE-798) và Đặc tả Xác thực (`docs/02-security-auth-spec.md §2-3`), microservice Outage Work Order API (`oms-api-demo`) hiện chỉ hỗ trợ xác thực cục bộ HTTP Basic Auth thông qua `InMemoryUserDetailsManager` trên các môi trường phát triển (`@Profile("!prod")`). Để đưa ứng dụng lên môi trường Production an toàn và tích hợp vào hệ sinh thái Identity Provider (IdP) tập trung của doanh nghiệp (như Keycloak, Okta hoặc Azure AD), hệ thống bắt buộc phải tích hợp **OAuth2 Resource Server** để xác thực các JWT Bearer Tokens.

Các ràng buộc kỹ thuật cốt lõi đạt chuẩn Oracle Java của dự án:
1. **Oracle Coding Standards & Explicit Code:** Mã nguồn Java 17 tường minh, cấm tuyệt đối Lombok, tiêm phụ thuộc qua Constructor Injection, sử dụng các kiểu dữ liệu bất biến và stream API an toàn.
2. **Oracle Javadoc JEP 106 Specification:** Toàn bộ lớp, hằng số, phương thức mới bắt buộc có Javadoc đầy đủ với các tag chuẩn mực: `@apiNote` (hướng dẫn tích hợp API), `@implSpec` (ràng buộc thuật toán và ánh xạ claims), `@implNote` (ghi chú hiệu năng/stateless), `@param`, `@return`, `@throws`, `@see`, `@since`.
3. **Kiến Trúc Xác Thực Song Song (Dual-Mode Authentication):**
   - Giữ nguyên cấu hình HTTP Basic Auth trên profile `!prod` để bảo vệ giao diện Web Test Console và toàn bộ 78+ unit/integration test cases hiện hữu không bị gián đoạn.
   - Bổ sung cấu hình `oauth2ResourceServer` vào `SecurityFilterChain` để giải mã và xác thực header `Authorization: Bearer <JWT>`.
4. **Chuẩn Hóa Ánh Xạ Phân Quyền (RBAC Role Normalization):**
   - JWT payload chứa claim `roles` (mảng danh sách quyền, ví dụ: `["ROLE_DISPATCHER", "ROLE_TECHNICIAN", "ROLE_ADMIN"]` hoặc `["DISPATCHER", "TECHNICIAN"]`).
   - Converter bắt buộc chuẩn hóa mọi authority có tiền tố `ROLE_` để khớp 100% với các annotation `@PreAuthorize("hasAnyRole(...)")` tại tầng Controller.
5. **Chuẩn Lỗi RFC 7807 Problem Details:**
   - Mọi vi phạm xác thực JWT (Token hết hạn, sai chữ ký, định dạng không hợp lệ) phải trả về HTTP 401 với `application/problem+json` và `type: urn:problem-type:unauthorized`.
   - Vi phạm phân quyền truy cập phải trả về HTTP 403 với `type: urn:problem-type:forbidden`.
6. **Bảo Toàn Chất Lượng JaCoCo Quality Gate:** Đạt 100% Line Coverage và 100% Branch Coverage cho toàn bộ mã nguồn mới.

---

# TASK:
Hiện thực hóa toàn diện giải pháp OAuth2 JWT Resource Server SEC-05 theo chuẩn Oracle Java qua 5 giai đoạn chi tiết sau:

### Giai Đoạn 1: Bổ Sung Dependency OAuth2 Resource Server vào `pom.xml`
Khai báo starter chính thức của Spring Boot ecosystem (không gắn cứng version):
```xml
<!-- Spring Boot OAuth2 Resource Server for Centralized JWT Auth (SEC-05, CWE-798) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Giai Đoạn 2: Hiện Thực Hóa `JwtRoleConverter.java` Đạt Chuẩn Oracle JEP 106
Tạo mới file tại `src/main/java/com/gpc/oms/config/JwtRoleConverter.java`:
1. **Giao diện cài đặt:** Cài đặt interface chuẩn `org.springframework.core.convert.converter.Converter<Jwt, Collection<GrantedAuthority>>`.
2. **Thuật Toán Trích Xuất & Chuẩn Hóa Claims trong `convert(Jwt jwt)`:**
   - Đọc danh sách vai trò từ claim `"roles"` (hỗ trợ cả `"roles"` hoặc fallback danh sách rỗng nếu null).
   - Duyệt qua từng role string, loại bỏ khoảng trắng thừa (`trim()`).
   - Đảm bảo tiền tố `ROLE_`: Nếu role chưa bắt đầu bằng `ROLE_`, tự động gắn tiền tố (`"ROLE_" + role.toUpperCase()`).
   - Chuyển đổi thành tập hợp `SimpleGrantedAuthority`.
   - Xử lý phòng vệ: Nếu claim rỗng hoặc null, trả về `Collections.emptyList()` thay vì ném NullPointerException.
3. **Oracle Javadoc:** Bổ sung Javadoc chi tiết cấp độ Class và Method với các tag `@apiNote`, `@implSpec`, `@implNote`, `@param`, `@return`.

### Giai Đoạn 3: Cấu Hình `SecurityConfig.java` Hỗ Trợ OAuth2 Resource Server
Tại `src/main/java/com/gpc/oms/config/SecurityConfig.java`:
1. Khai báo Bean `JwtAuthenticationConverter`:
   - Sử dụng `org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter`.
   - Thiết lập `setJwtGrantedAuthoritiesConverter(new JwtRoleConverter())`.
   - Thiết lập `setPrincipalClaimName("sub")`.
2. Bổ sung cấu hình `oauth2ResourceServer` vào `filterChain(HttpSecurity http, ...)`:
   - Kích hoạt JWT Resource Server:
     ```java
     .oauth2ResourceServer(oauth2 -> oauth2
         .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
         .authenticationEntryPoint((request, response, authException) -> {
             response.setStatus(401);
             response.setContentType("application/problem+json");
             response.getWriter().write("""
                 {"type":"%s","title":"Unauthorized","status":401,"detail":"%s","instance":"%s"}"""
                 .formatted(com.gpc.oms.exception.ProblemTypes.UNAUTHORIZED,
                            authException.getMessage() != null ? authException.getMessage() : "Authentication token is missing or expired",
                            request.getRequestURI()));
         })
     )
     ```
3. Đảm bảo hỗ trợ song song HTTP Basic Auth và OAuth2 Resource Server không gây xung đột.

### Giai Đoạn 4: Xây Dựng Bộ Kiểm Thử Tự Động Toàn Diện
1. **Unit Test Bộ Chuyển Đổi (`JwtRoleConverterTest.java`):**
   - Vị trí: `src/test/java/com/gpc/oms/config/JwtRoleConverterTest.java`.
   - Kịch bản kiểm thử:
     * Token có `roles = ["ROLE_ADMIN", "DISPATCHER"]` $\rightarrow$ Chuẩn hóa thành `["ROLE_ADMIN", "ROLE_DISPATCHER"]`.
     * Token không có claim `roles` (null) $\rightarrow$ Trả về danh sách rỗng an toàn.
     * Token có danh sách `roles` rỗng $\rightarrow$ Trả về danh sách rỗng.
     * Token có roles chứa khoảng trắng hoặc chữ thường $\rightarrow$ Được chuẩn hóa chính xác.
2. **Integration Security Test (`OAuth2JwtSecurityIntegrationTest.java`):**
   - Vị trí: `src/test/java/com/gpc/oms/config/OAuth2JwtSecurityIntegrationTest.java`.
   - Sử dụng `SecurityMockMvcRequestPostProcessors.jwt()`:
     * **Kịch Bản 1 (Valid JWT - DISPATCHER):** Gửi `POST /api/v1/workorders` kèm JWT claim `roles = ["ROLE_DISPATCHER"]` $\rightarrow$ Xác thực thành công, nhận HTTP 201 Created.
     * **Kịch Bản 2 (RBAC Violation):** Gửi `PATCH /api/v1/workorders/{id}/status` kèm JWT claim `roles = ["ROLE_DISPATCHER"]` $\rightarrow$ Nhận HTTP 403 Forbidden.
     * **Kịch Bản 3 (Valid JWT - TECHNICIAN):** Gửi `PATCH /api/v1/workorders/{id}/status` kèm JWT claim `roles = ["ROLE_TECHNICIAN"]` $\rightarrow$ Cho phép truy cập, không bị 401/403.
     * **Kịch Bản 4 (Unauthenticated Request):** Gửi request không có Authorization header $\rightarrow$ Nhận HTTP 401 Unauthorized kèm body RFC 7807.

### Giai Đoạn 5: Kiểm Toán Chất Lượng & JaCoCo Quality Gate
1. Chạy lệnh: `mvn clean verify`.
2. Xác nhận 100% test cases (toàn bộ test suite cũ và mới) đều PASS Green.
3. Xác nhận JaCoCo Quality Gate giữ vững 100% Line & Branch Coverage trên toàn bộ các gói nghiệp vụ.

---

# CONSTRAINTS:
1. **Chuẩn Oracle Java 17 & Stateless REST:**
   - 100% Stateless (`SessionCreationPolicy.STATELESS`).
   - Không sử dụng Lombok, mọi mapper và converter phải explicit, null-safe.
   - Tuân thủ JEP 106 JavaDoc tags.
2. **Tương Thích Ngược Tuyệt Đối (Zero Regressions):**
   - Không được làm gãy vỡ HTTP Basic Auth đang phục vụ các bộ test tích hợp và web console demo.
3. **Chuẩn Lỗi RFC 7807:**
   - Mọi phản hồi lỗi xác thực (401) hoặc phân quyền (403) phải trả về `application/problem+json`.
4. **Green Quality Gate:**
   - `mvn clean verify` đạt 100% (0 errors, 0 failures, 100% JaCoCo coverage).

---

# DONE WHEN:
1. `pom.xml` tích hợp thành công `spring-boot-starter-oauth2-resource-server`.
2. Lớp `JwtRoleConverter.java` được cài đặt hoàn chỉnh với Javadoc chuẩn Oracle JEP 106.
3. `SecurityConfig.java` cấu hình song song HTTP Basic Auth và OAuth2 Resource Server với `JwtAuthenticationConverter`.
4. Toàn bộ kịch bản kiểm thử JWT và RBAC trong `JwtRoleConverterTest.java` và `OAuth2JwtSecurityIntegrationTest.java` đều PASS 100%.
5. Lệnh `mvn clean verify` chạy thành công 100% Green (0 failure, 0 error, JaCoCo PASS 100%).
```
