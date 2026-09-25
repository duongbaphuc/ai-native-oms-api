// Nguồn gốc AI: sinh từ docs/02-api-spec.md §3, docs/00-api-rules.md §3
package com.gpc.oms.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử đơn vị cho ngoại lệ {@link ResourceNotFoundException}.
 */
@DisplayName("Kiểm thử đơn vị ngoại lệ ResourceNotFoundException")
class ResourceNotFoundExceptionTest {

    @Test
    @DisplayName("Constructor thiết lập chính xác thông điệp ngoại lệ và kế thừa RuntimeException")
    void constructor_setsMessageAndInheritance() {
        String message = "WorkOrder not found with id: 12345";
        ResourceNotFoundException ex = new ResourceNotFoundException(message);

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("forWorkOrder static factory method tạo ngoại lệ với định dạng thông điệp chuẩn")
    void forWorkOrder_createsExceptionWithStandardMessage() {
        java.util.UUID id = java.util.UUID.randomUUID();
        ResourceNotFoundException ex = ResourceNotFoundException.forWorkOrder(id);

        assertThat(ex.getMessage()).isEqualTo("WorkOrder not found with id: " + id);
    }
}
