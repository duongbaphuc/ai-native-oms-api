<!--
Role: Principal Security Architect & DevSecOps Engineer
Task: Define SecurityFilterChain, JWT Claims Schema, RBAC Matrix, Rate Limiting, and Testing Context
Context files: docs/00-security-rules.md, docs/00-api-rules.md, docs/00-coding-rules.md, docs/02-api-spec.md
Constraints: Spring Security 6.x (Spring Boot 3.3), Stateless REST, RFC 7807 for 401/403, zero-trust perimeter
Target Files:
- src/main/java/com/gpc/oms/config/SecurityConfig.java
- src/main/java/com/gpc/oms/config/JwtRoleConverter.java
- src/main/java/com/gpc/oms/config/RateLimitingFilter.java
- src/main/java/com/gpc/oms/config/CorrelationIdFilter.java
- src/main/java/com/gpc/oms/exception/GlobalExceptionHandler.java
-->
# Security, Authentication & Authorization Specification

Tài liệu này đặc tả chi tiết kiến trúc an ninh, cơ chế xác thực (Authentication), phân quyền (Authorization - RBAC), và các biện pháp phòng vệ biên giới mạng (Network Boundary Hardening) cho Outage Work Order API.

Tài liệu là kim chỉ nam để duy trì và kiểm chứng tính toàn vẹn của các lớp cấu hình `SecurityConfig`, `JwtRoleConverter`, `RateLimitingFilter`, `CorrelationIdFilter`, và các bộ kiểm thử bảo mật tầng WebMvcTest.

---

## 1. Tổng quan Kiến trúc Bảo mật (Security Architecture)

- **Framework:** Spring Security 6.3.4 tích hợp trên nền tảng Spring Boot 3.3.5.
- **Mô hình xác thực:** Không trạng thái (Stateless Session - `SessionCreationPolicy.STATELESS`).
- **Chiến lược xác thực đa môi trường (Defense-in-Depth):**
  - **Môi trường Non-Prod (Dev / Test / Staging - `@Profile("!prod")`):** HTTP Basic Authentication với danh sách tài khoản demo trong bộ nhớ (`InMemoryUserDetailsManager`).
  - **Môi trường Production (khi có `JwtDecoder` bean):** OAuth2 Resource Server xác thực JWT qua HTTP Header `Authorization: Bearer <token>` tích hợp `JwtRoleConverter` (PR #68). `filterChain` không được gắn trực tiếp `@Profile("prod")`; việc bật JWT phụ thuộc vào bean decoder.
- **Phòng chống DoS & Brute-force:** `RateLimitingFilter` sử dụng Bucket4j giới hạn 20 write / 60 read req/min cho mỗi IP, tự động trả về HTTP 429 RFC 7807 (PR #65).
- **Truy vết phân tán (Distributed Tracing):** `CorrelationIdFilter` gán mã UUID truy vết vào MDC log context và response header `X-Correlation-Id` (PR #67).
- **Cơ chế phân quyền:** Phân quyền theo vai trò (Role-Based Access Control - RBAC) sử dụng Method Security `@EnableMethodSecurity(prePostEnabled = true)`.
- **Kiến trúc Dual SecurityFilterChain (SEC-01 Hardening):**
  - **`h2ConsoleChain` (`@Order(1)`, `@Profile("!prod")`):** Chỉ kích hoạt ở môi trường non-prod, cho phép truy cập `/h2-console/**` công khai phục vụ kiểm thử và debug cục bộ.
  - **`filterChain` (`@Order(2)`):** Áp dụng cho toàn bộ endpoints nghiệp vụ. Cho phép truy cập công khai trang chủ (`/`, `/index.html`, `/favicon.ico`) và Actuator health/info. Trên profile `prod`, đường dẫn `/h2-console/**` và `/actuator/prometheus` yêu cầu bắt buộc quyền `ROLE_ADMIN`. Toàn bộ request còn lại yêu cầu xác thực.
- **Định dạng lỗi:** Toàn bộ vi phạm an ninh (401, 403, 429) bắt buộc trả về định dạng **RFC 7807 Problem Details** (`application/problem+json`).

---

## 2. JWT Claims Schema & Token Parsing

Hệ thống hỗ trợ parse token JWT từ Keycloak / OIDC Identity Provider với cấu trúc claims sau:

### Bảng Cấu trúc JWT Claims

| Tên Claim | Kiểu Dữ liệu | Bắt buộc | Ví dụ | Ý nghĩa & Quy tắc Xử lý |
|---|---|---|---|---|
| `sub` | `String` | Có | `"user-dispatch-01"` | Định danh người dùng (User ID / Username) |
| `iss` | `String` | Có | `"https://auth.gpc.com"` | Cơ quan phát hành Token (Token Issuer) |
| `roles` | `List<String>` | Tùy chọn | `["ROLE_DISPATCHER"]` | Mảng roles chuẩn mức gốc |
| `realm_access.roles` | `List<String>` | Tùy chọn | `["DISPATCHER"]` | Mảng Realm roles từ Keycloak (tự động chuẩn hóa tiền tố `ROLE_`) |
| `resource_access.*.roles` | `List<String>` | Tùy chọn | `["TECHNICIAN"]` | Mảng Resource/Client roles từ Keycloak |
| `iat` | `Long` | Có | `1774340000` | Thời điểm phát hành token (Epoch seconds) |
| `exp` | `Long` | Có | `1774343600` | Thời điểm hết hạn token (Epoch seconds) |

### Cơ chế Chuyển đổi Quyền (JwtRoleConverter)

Lớp `JwtRoleConverter` (cài đặt `Converter<Jwt, Collection<GrantedAuthority>>`) trích xuất và chuẩn hóa tất cả các vai trò từ cả 3 nguồn claim (`realm_access`, `resource_access`, và `roles`), đảm bảo tự động gắn tiền tố `ROLE_`:
```java
// JwtRoleConverter logic
public Collection<GrantedAuthority> convert(Jwt jwt) {
    Set<String> allRoles = new HashSet<>();
    extractRealmRoles(jwt, allRoles);
    extractResourceRoles(jwt, allRoles);
    extractSimpleRoles(jwt, allRoles);
    return allRoles.stream()
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toUnmodifiableSet());
}
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
- **Trạng thái implementation:** Chưa có `CorsConfigurationSource` hoặc `http.cors(...)` trong `SecurityConfig`; biến `APP_CORS_ALLOWED_ORIGINS` chưa được đọc từ code.
- Nếu bật CORS trong production, phải cấu hình origins/methods/headers qua secret-managed environment configuration trước khi nghiệm thu.

### 2. HTTP Security Headers
- **Đã triển khai:** `frameOptions().sameOrigin()` trong cả hai security chain để H2 Console hoạt động.
- **Chưa được chứng minh trong implementation:** CSP, HSTS và `X-Content-Type-Options` không được cấu hình tường minh trong source hiện tại.

### 3. CSRF Policy
- Vì API tuân thủ kiến trúc RESTful hoàn toàn Stateless (sử dụng Header JWT, không dùng Cookie Session), cấu hình `csrf.disable()` được phép áp dụng theo đúng chuẩn OWASP cho Token-based APIs.

---

## 5. Chính sách Giới hạn Tần suất (Rate Limiting Policy - SEC-06)

Nhằm ngăn ngừa tấn công DoS, Brute-force và kiểm soát mức độ tiêu thụ tài nguyên của API (CWE-770 & CWE-400):
- **Thuật toán cốt lõi:** Token Bucket (Bucket4j `com.bucket4j:bucket4j-core`).
- **Bộ nhớ đệm & Chiến lược Eviction:** Tích hợp **Caffeine Cache** (`com.github.ben-manes.caffeine:caffeine`) với cấu trúc `Cache<String, Bucket>` sở hữu chính sách Window TinyLFU / LRU eviction tự động:
  - Ngưỡng dung lượng tối đa: `MAX_CACHE_ENTRIES = 10,000`.
  - Thời gian hết hạn truy cập: `expireAfterAccess(Duration.ofMinutes(10))`.
  - **Triệt tiêu lỗ hổng DoS:** Tuyệt đối không sử dụng lệnh xóa toàn bộ `clear()`. Khi chạm ngưỡng dung lượng, Caffeine tự động loại bỏ các bucket không hoạt động của client cũ mà không ảnh hưởng hoặc "ân xá" cho các IP vi phạm đang bị chặn (throttled).
- **Phân giải định danh Client:** Dựa trên IP Address của client (`X-Forwarded-For` header hoặc `request.getRemoteAddr()`).
- **Phân tách chính sách Read / Write:**
  - **Thao tác Ghi (Write - `POST`, `PATCH`, `PUT`, `DELETE`):** 20 requests / phút / IP (nạp 20 tokens mỗi phút).
  - **Thao tác Đọc (Read - `GET`, `HEAD`, `OPTIONS`):** 60 requests / phút / IP (nạp 60 tokens mỗi phút).
- **Ranh giới Bỏ qua Bộ lọc (`shouldNotFilter`):**
  - Không áp dụng Rate Limiting cho `/actuator/**`, `/`, `/index.html`, `/favicon.ico`, `/h2-console/**`.
- **Xử lý khi vượt hạn mức (Rate Limit Exceeded):** Trả về HTTP `429 Too Many Requests` dạng RFC 7807 kèm Header `Retry-After`:
  ```json
  {
    "type": "urn:problem-type:rate-limit-exceeded",
    "title": "Too Many Requests",
    "status": 429,
    "detail": "Bạn đã vượt quá giới hạn tần suất gọi API. Vui lòng thử lại sau {retryAfterSeconds} giây.",
    "instance": "/api/v1/workorders"
  }
  ```

---

## 6. Xử lý Lỗi Tầng Bảo mật theo Chuẩn RFC 7807
 
 Các ngoại lệ bảo mật phát sinh tại tầng Filter hoặc Security Interceptor phải được chuyển đổi sang định dạng JSON chuẩn `application/problem+json`:
 
 ### 1. Chưa xác thực (HTTP 401 Unauthorized)
 - **Lớp cài đặt:** lambda `authenticationEntryPoint` trong `SecurityConfig.filterChain`
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
 - **Lớp cài đặt:** lambda `accessDeniedHandler` trong `SecurityConfig.filterChain`; `GlobalExceptionHandler` xử lý `AccessDeniedException` ở tầng method security.
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
