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
            .securityMatcher("/h2-console/**")
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/favicon.ico", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
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
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
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
