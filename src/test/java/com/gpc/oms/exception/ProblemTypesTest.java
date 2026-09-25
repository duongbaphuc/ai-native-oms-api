// Nguồn gốc AI: kiểm thử đơn vị cho các hằng số ProblemTypes
package com.gpc.oms.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho các hằng số URI định danh lỗi RFC 7807 trong {@link ProblemTypes}.
 */
@DisplayName("Kiểm thử đơn vị hằng số URI ProblemTypes")
class ProblemTypesTest {

    @Test
    @DisplayName("ProblemTypes định nghĩa chính xác các hằng số URN theo chuẩn RFC 7807")
    void constants_areConfiguredCorrectly() {
        assertThat(ProblemTypes.VALIDATION_ERROR).isEqualTo(URI.create("urn:problem-type:validation-error"));
        assertThat(ProblemTypes.MALFORMED_JSON).isEqualTo(URI.create("urn:problem-type:malformed-json"));
        assertThat(ProblemTypes.UNAUTHORIZED).isEqualTo(URI.create("urn:problem-type:unauthorized"));
        assertThat(ProblemTypes.FORBIDDEN).isEqualTo(URI.create("urn:problem-type:forbidden"));
        assertThat(ProblemTypes.NOT_FOUND).isEqualTo(URI.create("urn:problem-type:not-found"));
        assertThat(ProblemTypes.INVALID_STATE_TRANSITION)
                .isEqualTo(URI.create("urn:problem-type:invalid-state-transition"));
        assertThat(ProblemTypes.INTERNAL_ERROR).isEqualTo(URI.create("urn:problem-type:internal-error"));
    }

    @Test
    @DisplayName("Constructor riêng tư có thể triệu gọi qua Reflection để đạt độ bao phủ 100%")
    void privateConstructor_forCoverage() throws Exception {
        Constructor<ProblemTypes> constructor = ProblemTypes.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ProblemTypes instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
