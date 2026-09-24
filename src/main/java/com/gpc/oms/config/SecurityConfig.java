// AI Provenance: generated from docs/security-auth-spec.md, docs/security-rules.md
package com.gpc.oms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/favicon.ico", "/h2-console/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/problem+json");
                    response.getWriter().write("""
                        {"type":"urn:problem-type:unauthorized","title":"Unauthorized","status":401,"detail":"Authentication token is missing or expired","instance":"%s"}"""
                        .formatted(request.getRequestURI()));
                })
            )
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
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
