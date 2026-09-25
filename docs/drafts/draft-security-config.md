<!--
Role: Senior Engineer. Task: Tạo cấu hình SecurityConfig và bộ chuyển đổi JwtRoleConverter phục vụ bảo mật đa tầng, phân định môi trường và RBAC.
Context files: docs/00-security-rules.md, docs/02-security-auth-spec.md, docs/09-SECURITY_HANDOVER_REPORT.md
Constraints: 
- Dual SecurityFilterChain: @Order(1) H2 Console (profile !prod), @Order(2) main API chain.
- Phân định môi trường: HTTP Basic Auth trên !prod, OAuth2 Resource Server JWT trên prod (CWE-798 mitigation).
- RBAC Actuator: /actuator/health & info permitAll(), /actuator/prometheus hasRole('ADMIN').
- RFC 7807 AuthenticationEntryPoint (urn:problem-type:unauthorized) và AccessDeniedHandler (urn:problem-type:forbidden).
- JwtRoleConverter trích xuất claim 'roles' chuẩn hóa prefix 'ROLE_'.
- Pure Java 17, Zero Lombok, Constructor Injection, final modifiers.
DRAFT ONLY — scoring target, never wired into app.
-->
# Draft: Security Configuration & JWT Role Converter

## 1. Target Files

| File | Package | Path | Action | Mục Đích |
|---|---|---|---|---|
| `SecurityConfig.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/SecurityConfig.java` | NEW | Cấu hình Dual SecurityFilterChain, HTTP Basic, OAuth2 JWT Resource Server, RBAC, và RFC 7807 Exception Handlers |
| `JwtRoleConverter.java` | `com.gpc.oms.config` | `src/main/java/com/gpc/oms/config/JwtRoleConverter.java` | NEW | Trích xuất và chuẩn hóa authority với tiền tố `ROLE_` từ claim JWT |
| `H2ConsoleSecurityTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/H2ConsoleSecurityTest.java` | NEW | Kiểm thử cách ly console H2 trên môi trường dev vs chặn truy cập trên prod |
| `OAuth2JwtSecurityIntegrationTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/OAuth2JwtSecurityIntegrationTest.java` | NEW | Kiểm thử tích hợp xác thực OAuth2 JWT Bearer Token và Method Security |
| `ActuatorSecurityTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/ActuatorSecurityTest.java` | NEW | Kiểm thử phân quyền RBAC cho các Actuator endpoints (health vs prometheus) |
| `JwtRoleConverterTest.java` | `com.gpc.oms.config` | `src/test/java/com/gpc/oms/config/JwtRoleConverterTest.java` | NEW | Unit test giải mã các cấu trúc JWT claim và chuyển đổi vai trò |

---

## 2. Architecture & Step-by-Step Logic

```
   Incoming HTTP Request
            │
            ▼
┌───────────────────────────────┐
│ @Order(1) h2ConsoleChain     │  (Active only when Profile != "prod")
│ Matches: /h2-console/**       │  Permits all in dev/test for rapid database inspection
└──────────────┬────────────────┘
               │ (Non-matching requests pass through)
               ▼
┌───────────────────────────────┐
│ @Order(2) filterChain         │
│ - Public endpoints:           │  /, /index.html, /favicon.ico, /actuator/health, /actuator/info
│ - Admin only:                 │  /actuator/prometheus, /h2-console/** (on prod)
│ - WorkOrder API (/api/v1/**): │  Requires Authenticated
│                               │
│ Authentication Mechanisms:    │
│   1. HTTP Basic (Active on !prod for developer convenience & browser testing)
│   2. OAuth2 Resource Server   │  (Active if JwtDecoder bean is available / prod)
│      └── JwtAuthenticationConverter with JwtRoleConverter
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│ RFC 7807 Error Responses      │
│ - AuthenticationEntryPoint:   │  401 Unauthorized -> urn:problem-type:unauthorized
│ - AccessDeniedHandler:        │  403 Forbidden    -> urn:problem-type:forbidden
└───────────────────────────────┘
```

### Thuật toán trích xuất Role trong `JwtRoleConverter`:
1. Tiếp nhận đối tượng `Jwt`.
2. Đọc claim mảng `roles` thông qua `jwt.getClaimAsStringList("roles")`.
3. Nếu null hoặc rỗng, trả về `Collections.emptyList()`.
4. Duyệt từng phần tử trong danh sách:
   - Lọc bỏ chuỗi null hoặc rỗng (`!role.isBlank()`).
   - Cắt bỏ khoảng trắng thừa (`trim()`).
   - Nếu đã bắt đầu bằng `ROLE_`, giữ nguyên; ngược lại thêm tiền tố `ROLE_` và chuyển sang chữ hoa (`ROLE_` + `role.toUpperCase()`).
   - Đóng gói thành `SimpleGrantedAuthority`.
5. Thu thập thành danh sách bất biến `Collections.unmodifiableList(...)`.

> [!IMPORTANT]
> **Giảm Thiểu Lỗ Hổng Bảo Mật CWE-798 (Hardcoded Credentials):**
> Bean `UserDetailsService` (in-memory test users: admin/dispatcher/technician) được cô lập bằng `@Profile("!prod")`. Khi chạy trên môi trường production (`prod`), ứng dụng hoàn toàn không nạp tài khoản cục bộ tĩnh mà phụ thuộc 100% vào OAuth2 Resource Server và Identity Provider của doanh nghiệp.

> [!NOTE]
> **Dual FilterChain Matching:**
> `h2ConsoleChain` sử dụng `AntPathRequestMatcher.antMatcher("/h2-console/**")` và gán `@Order(1)`. Việc này đảm bảo tính năng kiểm tra cơ sở dữ liệu trên môi trường phát triển không bị ảnh hưởng bởi các quy tắc xác thực nghiêm ngặt của `filterChain` (`@Order(2)`).

---

## 3. Implementation Blueprint: `SecurityConfig.java`

```java
// AI Provenance: generated from docs/02-security-auth-spec.md, docs/00-security-rules.md
package com.gpc.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    @Order(1)
    @Profile("!prod")
    public SecurityFilterChain h2ConsoleChain(final HttpSecurity http) throws Exception {
        http
            .securityMatchers(matchers -> matchers.requestMatchers(
                AntPathRequestMatcher.antMatcher("/h2-console/**"),
                AntPathRequestMatcher.antMatcher("/h2-console")
            ))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(
            final HttpSecurity http,
            final org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.jwt.JwtDecoder> jwtDecoderProvider) throws Exception {
        
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/favicon.ico", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/h2-console/**"),
                    AntPathRequestMatcher.antMatcher("/h2-console")
                ).hasRole("ADMIN")
                .requestMatchers("/actuator/prometheus").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("""
                        {"type":"%s","title":"Unauthorized","status":401,"detail":"Authentication token is missing or expired","instance":"%s"}"""
                        .formatted(com.gpc.oms.exception.ProblemTypes.UNAUTHORIZED, request.getRequestURI()));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("""
                        {"type":"%s","title":"Forbidden","status":403,"detail":"Access Denied: You do not have permission to access this resource","instance":"%s"}"""
                        .formatted(com.gpc.oms.exception.ProblemTypes.FORBIDDEN, request.getRequestURI()));
                })
            )
            .httpBasic(Customizer.withDefaults());

        final org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("""
                        {"type":"%s","title":"Unauthorized","status":401,"detail":"%s","instance":"%s"}"""
                        .formatted(com.gpc.oms.exception.ProblemTypes.UNAUTHORIZED,
                                   authException.getMessage() != null ? authException.getMessage() : "Authentication token is missing or expired",
                                   request.getRequestURI()));
                })
            );
        }
        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        final var converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    @Profile("!prod")
    public UserDetailsService userDetailsService() {
        final UserDetails admin = User.withUsername("admin")
            .password("{noop}admin123")
            .roles("ADMIN", "DISPATCHER", "TECHNICIAN")
            .build();
        final UserDetails dispatcher = User.withUsername("dispatcher")
            .password("{noop}dispatcher123")
            .roles("DISPATCHER")
            .build();
        final UserDetails technician = User.withUsername("technician")
            .password("{noop}technician123")
            .roles("TECHNICIAN")
            .build();
        return new InMemoryUserDetailsManager(admin, dispatcher, technician);
    }
}
```

---

## 4. Implementation Blueprint: `JwtRoleConverter.java`

```java
// AI Provenance: generated from docs/02-security-auth-spec.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    public JwtRoleConverter() {
        super();
    }

    @Override
    public Collection<GrantedAuthority> convert(final Jwt jwt) {
        final List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .map(role -> role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }
}
```

---

## 5. Security Test Sketches

### Sketch 1: `H2ConsoleSecurityTest.java`
- **Mục tiêu:** Kiểm thử rằng trên profile dev (`!prod`), `/h2-console` trả về 200 OK, trong khi trên profile `prod`, truy cập ẩn danh bị chặn với 401/403.
- **Annotated with:** `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`.

### Sketch 2: `OAuth2JwtSecurityIntegrationTest.java`
- **Mục tiêu:** Xác thực rằng request mang Bearer Token JWT hợp lệ chứa role `DISPATCHER` hoặc `TECHNICIAN` có thể thao tác với WorkOrder API; token hết hạn hoặc giả mạo bị từ chối với RFC 7807 HTTP 401.
- **Annotated with:** `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)`, `@AutoConfigureMockMvc`, `@ActiveProfiles({"prod", "test"})`.

### Sketch 3: `ActuatorSecurityTest.java`
- **Mục tiêu:** Đảm bảo `/actuator/health` và `/actuator/info` mở công khai cho liveness/readiness probes của Kubernetes; `/actuator/prometheus` bắt buộc role `ADMIN`.

### Sketch 4: `JwtRoleConverterTest.java`
- **Mục tiêu:** Unit test cô lập kiểm tra việc nạp claim `roles` danh sách, xử lý role có hoặc không có tiền tố `ROLE_`, xử lý khoảng trắng, và xử lý claim rỗng hoặc null.
