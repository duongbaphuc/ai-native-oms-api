// Nguồn gốc AI: sinh từ docs/02-security-auth-spec.md, docs/09-SECURITY_HANDOVER_REPORT.md
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
 * Bộ chuyển đổi tùy chỉnh trích xuất và chuẩn hóa quyền vai trò RBAC từ JSON Web Tokens (JWT).
 *
 * <p>Giảm thiểu nguy cơ CWE-798 (Thông tin xác thực nhúng cứng) bằng cách kết nối các claims từ Nhà cung cấp
 * định danh (Identity Provider) bên ngoài với hạ tầng Method Security của Spring Security.</p>
 *
 * @apiNote Phân tích mảng claim {@code roles} từ đối tượng {@link Jwt}. Bảo đảm mọi vai trò trích xuất
 *          đều sở hữu tiền tố chuẩn mực {@code ROLE_} phục vụ kiểm tra qua {@code @PreAuthorize("hasAnyRole(...)")}
 *          và cơ chế đánh giá quyền RBAC của Spring Security.
 * @implSpec Triển khai {@link Converter} ánh xạ {@link Jwt} thành tập hợp không thể biến đổi
 *           của các đối tượng {@link GrantedAuthority}.
 *           Xử lý an toàn khi claims rỗng hoặc {@code null} bằng cách trả về {@link Collections#emptyList()}.
 * @implNote Singleton an toàn luồng (thread-safe) và phi trạng thái (stateless). Không lưu giữ ngữ cảnh bảo mật.
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 * @see org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
 * @see <a href="https://cwe.mitre.org/data/definitions/798.html">CWE-798</a>
 */
public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLES_CLAIM = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Constructor mặc định phục vụ việc khởi tạo bộ chuyển đổi.
     */
    public JwtRoleConverter() {
        super();
    }

    /**
     * Trích xuất và chuẩn hóa các vai trò từ mã JWT đã cung cấp thành danh sách các quyền hạn được cấp.
     *
     * @param jwt Đối tượng JSON Web Token đã được giải mã
     * @return Tập hợp không thể biến đổi chứa các đối tượng {@link GrantedAuthority} đã chuẩn hóa,
     *         hoặc tập hợp rỗng nếu không tồn tại claim vai trò
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
