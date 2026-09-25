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

/**
 * Custom converter extracting and normalizing RBAC role authorities from JSON Web Tokens (JWT).
 *
 * <p>Mitigates CWE-798 (Hardcoded / Local Credentials in Production Gap) by bridging external Identity
 * Provider (IdP) claims with Spring Security's Method Security infrastructure.</p>
 *
 * @apiNote Parses the {@code roles} claim array from a decoded {@link Jwt}. Ensures every extracted
 *          authority possesses the canonical {@code ROLE_} prefix required by {@code @PreAuthorize("hasAnyRole(...)")}
 *          and Spring Security's RBAC evaluate engine.
 * @implSpec Implements {@link Converter} mapping {@link Jwt} to an unmodifiable collection of {@link GrantedAuthority}.
 *           Safely handles {@code null} or empty claims by returning {@link Collections#emptyList()}.
 * @implNote Thread-safe and stateless singleton. Does not retain any claim or security state.
 * @author GPC OMS Architecture Team
 * @version 1.0.0
 * @since 1.0.0
 * @see org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
 * @see <a href="https://cwe.mitre.org/data/definitions/798.html">CWE-798</a>
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Default constructor for converter instantiation.
     */
    public JwtRoleConverter() {
        super();
    }

    /**
     * Extracts and normalizes roles from the provided JWT token into granted authorities.
     *
     * @param jwt The decoded JSON Web Token
     * @return An unmodifiable collection of normalized {@link GrantedAuthority} objects,
     *         or an empty collection if no roles are present
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        return roles.stream()
                .filter(role -> role != null && !role.isBlank())
                .map(String::trim)
                .map(role -> role.startsWith(ROLE_PREFIX) ? role : ROLE_PREFIX + role.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
    }
}
