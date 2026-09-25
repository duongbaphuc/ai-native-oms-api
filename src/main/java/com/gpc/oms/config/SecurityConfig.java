// Nguồn gốc AI: sinh từ docs/02-security-auth-spec.md, docs/00-security-rules.md
package com.gpc.oms.config;

import com.gpc.oms.exception.ProblemTypes;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Cấu hình bảo mật phân tầng (Layered Security Configuration) trên nền tảng Spring Security 6.
 *
 * <p>Thiết lập hai chuỗi lọc độc lập: chuỗi mở bảng điều khiển H2 Console (phi sản xuất)
 * và chuỗi bảo vệ tài nguyên API kết hợp OAuth2 Resource Server (JWT) và HTTP Basic.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * SEC-01: Bảng điều khiển H2 Console mở công khai chỉ ở môi trường phi sản xuất (!prod).
     * Chuỗi lọc này có mức ưu tiên cao hơn (Order 1) và chỉ áp dụng cho đường dẫn /h2-console/**.
     *
     * @param http Đối tượng cấu hình HttpSecurity của Spring
     * @return Chuỗi lọc {@link SecurityFilterChain} dành riêng cho H2 Console
     * @throws Exception nếu xảy ra lỗi trong quá trình cấu hình
     */
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

    /**
     * Chuỗi lọc bảo mật chính bảo vệ các endpoint API nghiệp vụ và giám sát hệ thống Actuator.
     *
     * @param http Đối tượng cấu hình HttpSecurity
     * @param jwtDecoderProvider Nhà cung cấp bean JwtDecoder (tùy chọn)
     * @return Chuỗi lọc {@link SecurityFilterChain} chính của ứng dụng
     * @throws Exception nếu xảy ra lỗi trong quá trình cấu hình
     */
    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(
            final HttpSecurity http,
            final ObjectProvider<JwtDecoder> jwtDecoderProvider) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/favicon.ico", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(
                    AntPathRequestMatcher.antMatcher("/h2-console/**"),
                    AntPathRequestMatcher.antMatcher("/h2-console")
                ).hasRole(RoleConstants.ADMIN)
                .requestMatchers("/actuator/prometheus").hasRole(RoleConstants.ADMIN)
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.getWriter().write("""
                        {"type":"%s",\
                        "title":"%s",\
                        "status":%d,\
                        "detail":"%s",\
                        "instance":"%s"}"""
                        .formatted(ProblemTypes.UNAUTHORIZED, ProblemTypes.TITLE_UNAUTHORIZED,
                                HttpStatus.UNAUTHORIZED.value(), ProblemTypes.DETAIL_UNAUTHORIZED_TOKEN,
                                request.getRequestURI()));
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.getWriter().write("""
                        {"type":"%s",\
                        "title":"%s",\
                        "status":%d,\
                        "detail":"%s",\
                        "instance":"%s"}"""
                        .formatted(ProblemTypes.FORBIDDEN, ProblemTypes.TITLE_FORBIDDEN,
                                HttpStatus.FORBIDDEN.value(), ProblemTypes.DETAIL_FORBIDDEN_PERMISSION,
                                request.getRequestURI()));
                })
            )
            .httpBasic(Customizer.withDefaults());

        final JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
        if (jwtDecoder != null) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    final String detailMsg = authException.getMessage() != null
                            ? authException.getMessage()
                            : ProblemTypes.DETAIL_UNAUTHORIZED_TOKEN;
                    response.getWriter().write("""
                        {"type":"%s",\
                        "title":"%s",\
                        "status":%d,\
                        "detail":"%s",\
                        "instance":"%s"}"""
                        .formatted(ProblemTypes.UNAUTHORIZED, ProblemTypes.TITLE_UNAUTHORIZED,
                                HttpStatus.UNAUTHORIZED.value(), detailMsg, request.getRequestURI()));
                })
            );
        }
        return http.build();
    }

    /**
     * Cấu hình bộ chuyển đổi xác thực JWT với ánh xạ vai trò RBAC
     * và trích xuất danh tính chủ thể.
     *
     * @return Đối tượng {@link JwtAuthenticationConverter} đã được thiết lập
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * Tài khoản người dùng mẫu phục vụ môi trường phi sản xuất (!prod)
     * để kiểm thử giao diện và API.
     *
     * @return Dịch vụ quản lý thông tin người dùng trong bộ nhớ {@link UserDetailsService}
     */
    @Bean
    @Profile("!prod")
    public UserDetailsService userDetailsService() {
        final UserDetails admin = User.withUsername("admin")
            .password("{noop}admin123")
            .roles(RoleConstants.ADMIN, RoleConstants.DISPATCHER, RoleConstants.TECHNICIAN)
            .build();
        final UserDetails dispatcher = User.withUsername("dispatcher")
            .password("{noop}dispatcher123")
            .roles(RoleConstants.DISPATCHER)
            .build();
        final UserDetails technician = User.withUsername("technician")
            .password("{noop}technician123")
            .roles(RoleConstants.TECHNICIAN)
            .build();
        return new InMemoryUserDetailsManager(admin, dispatcher, technician);
    }
}
