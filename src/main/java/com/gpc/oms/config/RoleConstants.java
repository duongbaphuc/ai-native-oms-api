// Nguồn gốc AI: sinh từ docs/00-coding-rules.md, docs/02-security-auth-spec.md
package com.gpc.oms.config;

/**
 * Tập trung các hằng số định danh vai trò phân quyền người dùng (RBAC Roles) và biểu thức SpEL tương ứng.
 *
 * <p>Loại bỏ hoàn toàn các chuỗi ký tự ma thuật (magic strings) phân tán trong cấu hình bảo mật
 * {@link SecurityConfig} và các annotations {@code @PreAuthorize} trên Controller.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
public final class RoleConstants {

    private RoleConstants() {
        // Ngăn chặn khởi tạo thực thể lớp tiện ích (Effective Java Item 4)
    }

    /** Tiền tố vai trò chuẩn của Spring Security. */
    public static final String ROLE_PREFIX = "ROLE_";

    /** Tên vai trò Quản trị viên hệ thống (không kèm tiền tố). */
    public static final String ADMIN = "ADMIN";

    /** Tên vai trò Điều độ viên lưới điện (không kèm tiền tố). */
    public static final String DISPATCHER = "DISPATCHER";

    /** Tên vai trò Kỹ thuật viên hiện trường (không kèm tiền tố). */
    public static final String TECHNICIAN = "TECHNICIAN";

    /** Quyền hạn Quản trị viên đầy đủ kèm tiền tố ROLE_. */
    public static final String ROLE_ADMIN = ROLE_PREFIX + ADMIN;

    /** Quyền hạn Điều độ viên đầy đủ kèm tiền tố ROLE_. */
    public static final String ROLE_DISPATCHER = ROLE_PREFIX + DISPATCHER;

    /** Quyền hạn Kỹ thuật viên đầy đủ kèm tiền tố ROLE_. */
    public static final String ROLE_TECHNICIAN = ROLE_PREFIX + TECHNICIAN;

    /** Biểu thức SpEL yêu cầu quyền Quản trị viên (ADMIN). */
    public static final String HAS_ROLE_ADMIN = "hasRole('" + ADMIN + "')";

    /** Biểu thức SpEL cho phép Kỹ thuật viên hoặc Quản trị viên. */
    public static final String HAS_ROLE_TECHNICIAN_OR_ADMIN =
            "hasAnyRole('" + TECHNICIAN + "', '" + ADMIN + "')";

    /** Biểu thức SpEL cho phép Điều độ viên, Kỹ thuật viên hoặc Quản trị viên. */
    public static final String HAS_ROLE_DISPATCHER_TECHNICIAN_OR_ADMIN =
            "hasAnyRole('" + DISPATCHER + "', '" + TECHNICIAN + "', '" + ADMIN + "')";
}
