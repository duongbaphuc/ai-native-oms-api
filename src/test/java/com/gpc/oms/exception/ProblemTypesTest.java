// AI Provenance: unit test for ProblemTypes constants
package com.gpc.oms.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProblemTypes Constants Unit Tests")
class ProblemTypesTest {

    @Test
    @DisplayName("ProblemTypes defines correct RFC 7807 URN constants")
    void constants_areConfiguredCorrectly() {
        assertThat(ProblemTypes.VALIDATION_ERROR).isEqualTo(URI.create("urn:problem-type:validation-error"));
        assertThat(ProblemTypes.MALFORMED_JSON).isEqualTo(URI.create("urn:problem-type:malformed-json"));
        assertThat(ProblemTypes.UNAUTHORIZED).isEqualTo(URI.create("urn:problem-type:unauthorized"));
        assertThat(ProblemTypes.FORBIDDEN).isEqualTo(URI.create("urn:problem-type:forbidden"));
        assertThat(ProblemTypes.NOT_FOUND).isEqualTo(URI.create("urn:problem-type:not-found"));
        assertThat(ProblemTypes.INVALID_STATE_TRANSITION).isEqualTo(URI.create("urn:problem-type:invalid-state-transition"));
        assertThat(ProblemTypes.INTERNAL_ERROR).isEqualTo(URI.create("urn:problem-type:internal-error"));
    }

    @Test
    @DisplayName("Private constructor can be invoked via reflection for 100% coverage")
    void privateConstructor_forCoverage() throws Exception {
        Constructor<ProblemTypes> constructor = ProblemTypes.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        ProblemTypes instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }
}
