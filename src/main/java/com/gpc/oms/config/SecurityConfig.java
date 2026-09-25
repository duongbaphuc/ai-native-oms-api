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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * SEC-01: H2 Console mở công khai chỉ ở non-prod (dev/test).
     * Chain này ưu tiên cao hơn và chỉ match /h2-console/**.
     */
    @Bean
    @Order(1)
    @Profile("!prod")
    public SecurityFilterChain h2ConsoleChain(HttpSecurity http) throws Exception {
        http
            .securityMatchers(matchers -> matchers.requestMatchers(
                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/h2-console/**"),
                org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/h2-console")
            ))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           org.springframework.beans.factory.ObjectProvider<org.springframework.security.oauth2.jwt.JwtDecoder> jwtDecoderProvider) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/favicon.ico", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(
                    org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/h2-console/**"),
                    org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher("/h2-console")
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

        org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder = jwtDecoderProvider.getIfAvailable();
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

    /**
     * Configures the JWT authentication converter with custom role mapping and principal claim resolution.
     *
     * @return Configured {@link org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter}
     */
    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        var converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    /**
     * Demo users KHONG dùng prod — chỉ load khi profile khác "prod"
     * (web test console + browser testing ở dev). Prod dùng JWT (lane riêng).
     */
    @Bean
    @Profile("!prod")
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.withUsername("admin")
            .password("{noop}admin123")
            .roles("ADMIN", "DISPATCHER", "TECHNICIAN")
            .build();
        UserDetails dispatcher = User.withUsername("dispatcher")
            .password("{noop}dispatcher123")
            .roles("DISPATCHER")
            .build();
        UserDetails technician = User.withUsername("technician")
            .password("{noop}technician123")
            .roles("TECHNICIAN")
            .build();
        return new InMemoryUserDetailsManager(admin, dispatcher, technician);
    }
}
