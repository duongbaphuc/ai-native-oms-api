<!--
Role: Principal Security Architect & DevSecOps Engineer
Task: Define SecurityFilterChain, JWT Claims Schema, RBAC Matrix, Rate Limiting, and Testing Context
Context files: docs/security-rules.md, docs/api-rules.md, docs/coding-rules.md, docs/api-spec.md
Constraints: Spring Security 6.x (Spring Boot 3.3), Stateless REST, RFC 7807 for 401/403, zero-trust perimeter
Target Files:
- src/main/java/com/gpc/oms/config/SecurityConfig.java
- src/main/java/com/gpc/oms/config/JwtAuthenticationConverter.java
- src/main/java/com/gpc/oms/security/CustomAuthenticationEntryPoint.java
- src/main/java/com/gpc/oms/security/CustomAccessDeniedHandler.java
-->
# Security, Authentication & Authorization Specification

Tài liệu này đặc tả chi tiết kiến trúc an ninh, cơ chế xác thực (Authentication), phân quyền (Authorization - RBAC), và các biện pháp phòng vệ biên giới mạng (Network Boundary Hardening) cho Outage Work Order API.

Tài liệu là kim chỉ nam để GitHub Copilot tự động sinh các lớp cấu hình `SecurityConfig`, `JwtAuthenticationFilter`, `CustomAccessDeniedHandler`, và các bộ kiểm thử bảo mật tầng WebMvcTest.

---

## 1. Tổng quan Kiến trúc Bảo mật (Security Architecture)

- **Framework:** Spring Security 6.3.3 tích hợp trên nền tảng Spring Boot 3.3.4.
- **Mô hình xác thực:** Không trạng thái (Stateless Session - `SessionCreationPolicy.STATELESS`).
- **Chiến lược xác thực theo giai đoạn:**
  - **Hiện tại (Dev / Test / Staging):** HTTP Basic Authentication với phân định profile `@Profile("!prod")` cho danh sách tài khoản demo trong bộ nhớ (`InMemoryUserDetailsManager`).
  - **Mục tiêu Production (Enterprise Roadmap - Issue #33):** OAuth2 Resource Server xác thực JWT qua HTTP Header `Authorization: Bearer <token>`.
- **Cơ chế phân quyền:** Phân quyền theo vai trò (Role-Based Access Control - RBAC) sử dụng Method Security `@EnableMethodSecurity(prePostEnabled = true)`.
- **Kiến trúc Dual SecurityFilterChain (SEC-01 Hardening):**
  - **`h2ConsoleChain` (`@Order(1)`, `@Profile("!prod")`):** Chỉ kích hoạt ở môi trường non-prod, cho phép truy cập `/h2-console/**` công khai phục vụ kiểm thử và debug cục bộ.
  - **`filterChain` (`@Order(2)`):** Áp dụng cho toàn bộ endpoints nghiệp vụ. Cho phép truy cập công khai trang chủ (`/`, `/index.html`, `/favicon.ico`). Trên profile `prod`, đường dẫn `/h2-console/**` yêu cầu bắt buộc quyền `ROLE_ADMIN`. Toàn bộ request còn lại yêu cầu xác thực.
- **Định dạng lỗi:** Toàn bộ vi phạm an ninh (401, 403, 429) bắt buộc trả về định dạng **RFC 7807 Problem Details** (`application/problem+json`).

---

## 2. JWT Claims Schema & Token Parsing

Hệ thống kỳ vọng JWT hợp lệ chứa các claims nghiệp vụ tối thiểu sau:

### Bảng Cấu trúc JWT Claims

| Tên Claim | Kiểu Dữ liệu | Bắt buộc | Ví dụ | Ý nghĩa & Quy tắc Xử lý |
|---|---|---|---|---|
| `sub` | `String` | Có | `"user-dispatch-01"` | Định danh người dùng (User ID / Username) |
| `iss` | `String` | Có | `"https://auth.gpc.com"` | Cơ quan phát hành Token (Token Issuer) |
| `roles` | `List<String>` | Có | `["ROLE_DISPATCHER"]` | Danh sách quyền hạn. Bắt buộc có tiền tố `ROLE_` |
| `tenant_id` | `String` | Tùy chọn | `"VN-EVN-01"` | Mã định danh đơn vị điện lực (Multi-tenancy) |
| `iat` | `Long` | Có | `1774340000` | Thời điểm phát hành token (Epoch seconds) |
| `exp` | `Long` | Có | `1774343600` | Thời điểm hết hạn token (Epoch seconds) |

### Cơ chế Chuyển đổi Quyền (JwtAuthenticationConverter)

Khi trích xuất JWT, hệ thống phân tích mảng `roles` thành danh sách `GrantedAuthority`:
```java
// Logic chuyển đổi claims: mảng "roles" -> Collection<GrantedAuthority>
List<String> roles = jwt.getClaimAsStringList("roles");
Collection<GrantedAuthority> authorities = roles.stream()
    .map(SimpleGrantedAuthority::new)
    .collect(Collectors.toList());
```

---

## 3. Ma trận Phân quyền (RBAC Matrix)

Dự án xác định 3 vai trò chính trong hệ thống điều hành mất điện:
- **`ROLE_DISPATCHER`:** Điều độ viên lưới điện (Tạo mới phiếu sự cố, tra cứu toàn bộ danh sách).
- **`ROLE_TECHNICIAN`:** Kỹ thuật viên hiện trường (Xem danh sách, xem chi tiết và cập nhật tiến độ công việc).
- **`ROLE_ADMIN`:** Quản trị viên hệ thống (Toàn quyền quản trị và giám sát).

### Bảng Ma trận Kiểm soát Truy cập Endpoint

| HTTP Method | URI Pattern | Vai trò Cho phép (RBAC Rule) | Annotation Ràng buộc |
|---|---|---|---|
| `POST` | `/api/v1/workorders` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` |
| `GET` | `/api/v1/workorders` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` |
| `GET` | `/api/v1/workorders/{id}` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` |
| `PATCH` | `/api/v1/workorders/{id}/status`| `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")` |
| `GET` | `/actuator/health` | Public (Mọi truy cập) | `permitAll()` |
| `GET` | `/actuator/prometheus` | Internal / Admin | `@PreAuthorize("hasRole('ADMIN')")` |

---

## 4. Web Security & Phòng vệ Mạng (Network Boundary)

### 1. Cross-Origin Resource Sharing (CORS) Policy
- Cấu hình qua biến môi trường: `APP_CORS_ALLOWED_ORIGINS` (mặc định: `http://localhost:3000,http://localhost:8080`).
- **Allowed Methods:** `GET, POST, PATCH, OPTIONS`.
- **Allowed Headers:** `Authorization, Content-Type, X-Correlation-Id`.
- **Max Age:** `3600` giây (1 giờ).

### 2. HTTP Security Headers
Bắt buộc kích hoạt trên toàn bộ response:
- `Content-Security-Policy: default-src 'self'`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security: max-age=31536000; includeSubDomains`

### 3. CSRF Policy
- Vì API tuân thủ kiến trúc RESTful hoàn toàn Stateless (sử dụng Header JWT, không dùng Cookie Session), cấu hình `csrf.disable()` được phép áp dụng theo đúng chuẩn OWASP cho Token-based APIs.

---

## 5. Chính sách Giới hạn Tần suất (Rate Limiting Policy)

Nhằm ngăn ngừa tấn công Brute-force và quá tải hệ thống điều độ:
- **Thuật toán:** Token Bucket (Bucket4j).
- **Cấu hình theo đối tượng:**
  - **Người dùng đã xác thực (Authenticated):** 100 requests / phút / User ID.
  - **Người dùng chưa xác thực (Unauthenticated):** 20 requests / phút / IP Address.
- **Xử lý khi vượt hạn mức:** Trả về HTTP `429 Too Many Requests` kèm RFC 7807:
  ```json
  {
    "type": "urn:problem-type:rate-limit-exceeded",
    "title": "Too Many Requests",
    "status": 429,
    "detail": "Bạn đã vượt quá giới hạn 100 requests/phút. Vui lòng thử lại sau.",
    "instance": "/api/v1/workorders"
  }
  ```

---

## 6. Xử lý Lỗi Tầng Bảo mật theo Chuẩn RFC 7807
 
 Các ngoại lệ bảo mật phát sinh tại tầng Filter hoặc Security Interceptor phải được chuyển đổi sang định dạng JSON chuẩn `application/problem+json`:
 
 ### 1. Chưa xác thực (HTTP 401 Unauthorized)
 - **Lớp cài đặt:** `CustomAuthenticationEntryPoint` (tại `SecurityConfig.filterChain`)
 - **RFC 7807 Payload:**
   ```json
   {
     "type": "urn:problem-type:unauthorized",
     "title": "Unauthorized",
     "status": 401,
     "detail": "Authentication token is missing or expired",
     "instance": "/api/v1/workorders"
   }
   ```
 
 ### 2. Không có quyền truy cập (HTTP 403 Forbidden)
 - **Lớp cài đặt:** `GlobalExceptionHandler.handleAccessDenied` (bắt `org.springframework.security.access.AccessDeniedException`)
 - **RFC 7807 Payload:**
   ```json
   {
     "type": "urn:problem-type:forbidden",
     "title": "Access Denied",
     "status": 403,
     "detail": "Access Denied",
     "instance": "/api/v1/workorders"
   }
   ```

---

## 7. Quy chuẩn Kiểm thử Bảo mật (MockMvc Security Testing)

Mọi endpoint trong dự án bắt buộc phải có tối thiểu 3 test case bảo mật:

```java
// 1. Success case: Đúng quyền DISPATCHER được phép tạo WorkOrder
@Test
@WithMockUser(username = "dispatcher-01", roles = {"DISPATCHER"})
void createWorkOrder_withDispatcherRole_shouldReturn201() throws Exception { ... }

// 2. Forbidden case: Sai quyền (vd: GUEST) gọi API POST tạo WorkOrder -> 403 Forbidden
@Test
@WithMockUser(username = "guest-01", roles = {"GUEST"})
void createWorkOrder_withGuestRole_shouldReturn403Forbidden() throws Exception { ... }

// 3. Unauthorized case: Không truyền token xác thực -> 401 Unauthorized
@Test
void createWorkOrder_unauthenticated_shouldReturn401Unauthorized() throws Exception { ... }
```
