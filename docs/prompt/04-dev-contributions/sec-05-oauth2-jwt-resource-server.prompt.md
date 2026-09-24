# Prompt: Integrate OAuth2 Resource Server for Centralized JWT Authentication (Issue #33 / SEC-05)
> **Phân loại:** Enterprise Security Feature Task Prompt  
> **Issue:** [#33](https://github.com/duongbaphuc/ai-native-oms-api/issues/33)  
> **Lỗ hổng:** SEC-05 (CWE-798 Hardcoded / Local Credentials in Production Gap — CVSS HIGH)  
> **Tài liệu đối chiếu:** `docs/security-auth-spec.md §2-3`, `docs/security-rules.md §1, §3`, `docs/SECURITY_HANDOVER_REPORT.md §3`  
> **Mục tiêu:** Tích hợp `spring-boot-starter-oauth2-resource-server`, hiện thực hóa cơ chế xác thực JWT không trạng thái (Stateless) tập trung với Identity Provider (Keycloak / Azure AD), bảo đảm an ninh cho môi trường Production.

```markdown
# ROLE:
Bạn là một "Senior Enterprise Security Architect" kiêm "Spring Security 6.x Specialist" chuyên sâu về chuẩn giao thức OAuth2 Resource Server, JWT (JSON Web Token) Claim Parsing, và kiến trúc phân quyền RBAC phân tán.

---

# TASK:
Hiện thực hóa tính năng xác thực JWT tập trung SEC-05 theo yêu cầu trong Issue #33 (`docs/security-auth-spec.md §2-3`):

1. **Bổ sung OAuth2 Resource Server Starter vào `pom.xml`:**
   - Trong `pom.xml`, bổ sung dependency chính thức:
     ```xml
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
     </dependency>
     ```
   - Không chỉ định version cứng (kế thừa tự động từ `spring-boot-starter-parent:3.3.4`).

2. **Hiện thực hóa `JwtAuthenticationConverter` trích xuất Roles:**
   - Xây dựng lớp hoặc Bean `JwtAuthenticationConverter` để ánh xạ danh sách quyền từ JWT claims sang Spring Security Authorities:
     * Đọc claim mảng `roles` từ token payload (ví dụ: `["ROLE_DISPATCHER", "ROLE_TECHNICIAN", "ROLE_ADMIN"]` hoặc `["DISPATCHER", "TECHNICIAN"]`).
     * Đảm bảo mọi authority đều có tiền tố chuẩn `ROLE_` tương thích với các annotation `@PreAuthorize("hasAnyRole(...)")` đang sử dụng tại Controller.
     * Trích xuất thuộc tính `sub` hoặc `preferred_username` làm định danh `Principal.getName()`.

3. **Cập nhật `SecurityConfig.java` hỗ trợ song song JWT và Dev Auth:**
   - Trong `src/main/java/com/gpc/oms/config/SecurityConfig.java`:
     * Bổ sung cấu hình OAuth2 Resource Server vào chuỗi bộ lọc:
       ```java
       .oauth2ResourceServer(oauth2 -> oauth2
           .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
           .authenticationEntryPoint(customAuthenticationEntryPoint) // trả về RFC 7807 401 khi token lỗi/hết hạn
       )
       ```
     * Giữ nguyên hỗ trợ HTTP Basic (`httpBasic(Customizer.withDefaults())`) cho profile phát triển (`!prod`) để không làm gián đoạn Web Test Console và bộ test kiểm thử hiện hữu.
     * Xử lý lỗi Token hết hạn, sai chữ ký, hoặc thiếu Header thành chuẩn lỗi RFC 7807 (`status: 401`, `type: urn:problem-type:unauthorized`, `title: Unauthorized`).

4. **Xây dựng Bộ Kiểm Thử Tự Động Xác Thực JWT (OAuth2 Security Tests):**
   - Viết test class kiểm chứng cơ chế xác thực JWT (ví dụ: `OAuth2JwtSecurityTest.java` tại `src/test/java/com/gpc/oms/config/` sử dụng `SecurityMockMvcRequestPostProcessors.jwt()`):
     * **Test case 1 (Valid JWT - Dispatcher):** Gửi request `POST /api/v1/workorders` kèm JWT hợp lệ có claim `roles = ["ROLE_DISPATCHER"]` $\rightarrow$ Nhận HTTP `201 Created`.
     * **Test case 2 (RBAC Violation):** Gửi request `PATCH /api/v1/workorders/{id}/status` kèm JWT có claim `roles = ["ROLE_DISPATCHER"]` $\rightarrow$ Nhận HTTP `403 Forbidden` (`urn:problem-type:forbidden`).
     * **Test case 3 (Valid JWT - Technician):** Gửi request `PATCH /api/v1/workorders/{id}/status` kèm JWT có claim `roles = ["ROLE_TECHNICIAN"]` $\rightarrow$ Xác thực và phân quyền thành công (không bị 401/403).
     * **Test case 4 (Invalid / Missing Token):** Gửi request không có token hoặc token sai định dạng $\rightarrow$ Nhận HTTP `401 Unauthorized` (`urn:problem-type:unauthorized`).

5. **Bảo toàn Quality Gate 100% JaCoCo Coverage:**
   - Mọi phương thức chuyển đổi claims và nhánh rẽ xử lý converter phải được bao phủ kiểm thử 100%.
   - Toàn bộ 75 test cases hiện tại tiếp tục PASS 100%.

---

# CONSTRAINTS:
1. **Stateless Zero-Trust Architecture:** Cơ chế xác thực JWT phải hoàn toàn không trạng thái (`SessionCreationPolicy.STATELESS`), sẵn sàng tích hợp với Keycloak, Okta, hoặc Azure AD B2C.
2. **Backward Compatibility:** Không được làm hỏng các bài kiểm thử slice `@WebMvcTest` đang dùng `@WithMockUser` và không ảnh hưởng đến tài khoản demo chạy trên môi trường local dev.
3. **Chuẩn Lỗi RFC 7807:** Mọi trường hợp từ chối xác thực JWT phải xuất ra định dạng `application/problem+json`.
4. **Green Build:** Toàn bộ test suite phải hoàn thành thành công với lệnh `mvn clean verify`.

---

# DONE WHEN:
1. `pom.xml` được bổ sung `spring-boot-starter-oauth2-resource-server`.
2. Hệ thống xác thực thành công các request mang Bearer JWT hợp lệ và ánh xạ đúng ma trận quyền hạn RBAC.
3. Bộ test kiểm thử JWT tự động hoàn thành 100% Green.
4. Lệnh `mvn clean verify` chạy thành công (0 failure, 0 error, JaCoCo PASS 100%).
```
