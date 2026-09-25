// Nguồn gốc AI: kiểm thử đơn vị cho RoleConstants
package com.gpc.oms.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho lớp tiện ích {@link RoleConstants}.
 */
@DisplayName("Kiểm thử đơn vị RoleConstants")
class RoleConstantsTest {

    @Test
    @DisplayName("RoleConstants định nghĩa chính xác các hằng số vai trò và biểu thức SpEL")
    void constants_areConfiguredCorrectly() {
        assertThat(RoleConstants.ROLE_PREFIX).isEqualTo("ROLE_");
        assertThat(RoleConstants.ADMIN).isEqualTo("ADMIN");
        assertThat(RoleConstants.DISPATCHER).isEqualTo("DISPATCHER");
        assertThat(RoleConstants.TECHNICIAN).isEqualTo("TECHNICIAN");

        assertThat(RoleConstants.ROLE_ADMIN).isEqualTo("ROLE_ADMIN");
        assertThat(RoleConstants.ROLE_DISPATCHER).isEqualTo("ROLE_DISPATCHER");
        assertThat(RoleConstants.ROLE_TECHNICIAN).isEqualTo("ROLE_TECHNICIAN");

        assertThat(RoleConstants.HAS_ROLE_ADMIN).isEqualTo("hasRole('ADMIN')");
        assertThat(RoleConstants.HAS_ROLE_TECHNICIAN_OR_ADMIN)
                .isEqualTo("hasAnyRole('TECHNICIAN', 'ADMIN')");
        assertThat(RoleConstants.HAS_ROLE_DISPATCHER_TECHNICIAN_OR_ADMIN)
                .isEqualTo("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')");
    }

    @Test
    @DisplayName("Constructor riêng tư có thể triệu gọi qua Reflection để đạt độ bao phủ 100%")
    void privateConstructor_forCoverage() throws Exception {
        Constructor<RoleConstants> constructor = RoleConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        RoleConstants instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
