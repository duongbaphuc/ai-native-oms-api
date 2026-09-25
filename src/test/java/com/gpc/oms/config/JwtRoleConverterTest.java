// Nguồn gốc AI: sinh từ docs/02-security-auth-spec.md, docs/09-SECURITY_HANDOVER_REPORT.md
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
 * Bộ kiểm thử đơn vị cho {@link JwtRoleConverter}.
 *
 * <p>Kiểm tra trích xuất claim vai trò, chuẩn hóa tiền tố ROLE_, xử lý an toàn giá trị null
 * và làm sạch khoảng trắng thừa.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@DisplayName("Kiểm thử đơn vị bộ chuyển đổi JwtRoleConverter")
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
    @DisplayName("Chuẩn hóa và trích xuất danh sách vai trò")
    class RoleNormalizationTests {

        @Test
        @DisplayName("Chuyển đổi các vai trò có hoặc không có tiền tố ROLE_ thành GrantedAuthority chuẩn hóa")
        void convert_normalizesRolesWithPrefix() {
            Jwt jwt = createJwtWithClaims(Map.of("roles", List.of("ROLE_ADMIN", "DISPATCHER", "technician")));

            Collection<GrantedAuthority> authorities = converter.convert(jwt);

            List<String> authorityNames = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            assertThat(authorityNames).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_DISPATCHER", "ROLE_TECHNICIAN");
        }

        @Test
        @DisplayName("Trả về tập hợp rỗng khi claim roles có giá trị null")
        void convert_whenRolesClaimIsNull_returnsEmptyCollection() {
            Jwt jwt = createJwtWithClaims(Map.of("sub", "user-123"));

            Collection<GrantedAuthority> authorities = converter.convert(jwt);

            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Trả về tập hợp rỗng khi claim roles là một danh sách rỗng")
        void convert_whenRolesClaimIsEmpty_returnsEmptyCollection() {
            Jwt jwt = createJwtWithClaims(Map.of("roles", Collections.emptyList()));

            Collection<GrantedAuthority> authorities = converter.convert(jwt);

            assertThat(authorities).isEmpty();
        }

        @Test
        @DisplayName("Lọc bỏ các phần tử rỗng, trắng hoặc null trong danh sách roles")
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
