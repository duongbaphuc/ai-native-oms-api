// AI Provenance: generated from docs/api-spec.md §3, docs/api-rules.md §3
package com.gpc.oms.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceNotFoundException Unit Tests")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Constructor sets exception message and extends RuntimeException")
    void constructor_setsMessageAndInheritance() {
        String message = "WorkOrder not found with id: 12345";
        ResourceNotFoundException ex = new ResourceNotFoundException(message);

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo(message);
    }
}
