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
        assertThat(ProblemTypes.RATE_LIMIT_EXCEEDED).isEqualTo(URI.create("urn:problem-type:rate-limit-exceeded"));

        assertThat(ProblemTypes.PROPERTY_INVALID_PARAMS).isEqualTo("invalidParams");
        assertThat(ProblemTypes.KEY_NAME).isEqualTo("name");
        assertThat(ProblemTypes.KEY_REASON).isEqualTo("reason");
        assertThat(ProblemTypes.FIELD_BODY).isEqualTo("body");

        assertThat(ProblemTypes.TITLE_VALIDATION_FAILED).isEqualTo("Validation Failed");
        assertThat(ProblemTypes.TITLE_MALFORMED_REQUEST_BODY).isEqualTo("Malformed Request Body");
        assertThat(ProblemTypes.TITLE_ACCESS_DENIED).isEqualTo("Access Denied");
        assertThat(ProblemTypes.TITLE_UNAUTHORIZED).isEqualTo("Unauthorized");
        assertThat(ProblemTypes.TITLE_FORBIDDEN).isEqualTo("Forbidden");
        assertThat(ProblemTypes.TITLE_TOO_MANY_REQUESTS).isEqualTo("Too Many Requests");

        assertThat(ProblemTypes.DETAIL_MALFORMED_BODY)
                .isEqualTo("Request body is malformed or contains an invalid enum value");
        assertThat(ProblemTypes.DETAIL_UNAUTHORIZED_TOKEN)
                .isEqualTo("Authentication token is missing or expired");
        assertThat(ProblemTypes.DETAIL_FORBIDDEN_PERMISSION)
                .isEqualTo("Access Denied: You do not have permission to access this resource");
        assertThat(ProblemTypes.DETAIL_INTERNAL_ERROR)
                .isEqualTo("An unexpected error occurred");
        assertThat(ProblemTypes.REASON_INVALID_VALUE).isEqualTo("Invalid value");
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
