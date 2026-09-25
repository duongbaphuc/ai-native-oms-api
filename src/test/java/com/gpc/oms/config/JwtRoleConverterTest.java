// AI Provenance: generated from docs/02-security-auth-spec.md, docs/09-SECURITY_HANDOVER_REPORT.md
package com.gpc.oms.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for {@link JwtRoleConverter}.
 *
 * @apiNote Verifies role claim extraction, prefix normalization, null safety, and whitespace sanitization.
 * @author GPC OMS Architecture Team
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("JwtRoleConverter Unit Tests")
class JwtRoleConverterTest {

    private JwtRoleConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtRoleConverter();
    }

    private Jwt createJwtWithClaims(Map<String, Object> claims) {
        return new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                claims
        );
    }

    @Nested
    @DisplayName("Role Normalization and Extraction")
    class RoleNormalizationTests {

        @Test
        @DisplayName("Converts roles with and without ROLE_ prefix into normalized GrantedAuthorities")
        void convert_normalizesRolesWithPrefix() {
            Jwt jwt = createJwtWithClaims(Map.of("roles", List.of("ROLE_ADMIN", "DISPATCHER", "technician")));

            Collection<GrantedAuthority> authorities = converter.convert(jwt);

            List<String> authorityNames = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            assertThat(authorityNames).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_DISPATCHER", "ROLE_TECHNICIAN");
        }

        @Test
        @DisplayName("Returns empty collection when roles claim is null")
        void convert_whenRolesClaimIsNull_returnsEmptyCollection() {
            Jwt jwt = createJwtWithClaims(Map.of("sub", "user-123"));

            Collection<GrantedAuthority> authorities = converter.convert(jwt);

            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Returns empty collection when roles claim is empty list")
        void convert_whenRolesClaimIsEmpty_returnsEmptyCollection() {
            Jwt jwt = createJwtWithClaims(Map.of("roles", Collections.emptyList()));

            Collection<GrantedAuthority> authorities = converter.convert(jwt);

            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Filters out blank and null elements within roles list")
        void convert_filtersBlankAndNullElements() {
            Jwt jwt = createJwtWithClaims(Map.of("roles", Arrays.asList("ROLE_ADMIN", "   ", null, "dispatcher")));

            Collection<GrantedAuthority> authorities = converter.convert(jwt);

            List<String> authorityNames = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            assertThat(authorityNames).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_DISPATCHER");
        }
    }
}
