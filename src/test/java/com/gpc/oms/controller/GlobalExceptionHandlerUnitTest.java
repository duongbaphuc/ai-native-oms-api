// Nguồn gốc AI: sinh từ docs/00-api-rules.md §2, docs/00-security-rules.md §4
package com.gpc.oms.controller;

import com.gpc.oms.exception.GlobalExceptionHandler;
import com.gpc.oms.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Bộ kiểm thử đơn vị trực tiếp cho {@link GlobalExceptionHandler} xác thực chuẩn RFC 7807.
 */
@DisplayName("Kiểm thử đơn vị bộ xử lý ngoại lệ GlobalExceptionHandler (Xác thực RFC 7807)")
class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Handler #1: MethodArgumentNotValidException ánh xạ thành 400 validation-error")
    void handleValidationErrors_withNonNullAndNullDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "workOrderRequest");

        // Nhánh 1: error.getDefaultMessage() != null
        bindingResult.addError(new FieldError("workOrderRequest", "equipmentId", "equipmentId must not be blank"));

        // Nhánh 2: error.getDefaultMessage() == null (kích hoạt fallback "Invalid value")
        bindingResult.addError(new FieldError("workOrderRequest", "description", null, false, null, null, null));

        when(ex.getBindingResult()).thenReturn(bindingResult);

        ProblemDetail problem = handler.handleValidationErrors(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Validation Failed");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:validation-error"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> invalidParams = (List<Map<String, String>>) problem.getProperties().get("invalidParams");
        assertThat(invalidParams).hasSize(2);
        assertThat(invalidParams.get(0)).containsEntry("name", "equipmentId")
                                        .containsEntry("reason", "equipmentId must not be blank");
        assertThat(invalidParams.get(1)).containsEntry("name", "description")
                                        .containsEntry("reason", "Invalid value");
    }

    @Test
    @DisplayName("Handler #2: HttpMessageNotReadableException ánh xạ thành 400 malformed-json")
    void handleMalformedJson_returnsProblemDetail() {
        @SuppressWarnings("deprecation")
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON parse error");

        ProblemDetail problem = handler.handleMalformedJson(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Malformed Request Body");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:malformed-json"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> invalidParams = (List<Map<String, String>>) problem.getProperties().get("invalidParams");
        assertThat(invalidParams).hasSize(1);
        assertThat(invalidParams.get(0)).containsEntry("name", "body")
                                        .containsEntry("reason", "Request body is malformed or contains an invalid enum value");
    }

    @Test
    @DisplayName("Handler #3: AccessDeniedException ánh xạ thành 403 forbidden")
    void handleAccessDenied_returnsProblemDetail() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ProblemDetail problem = handler.handleAccessDenied(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getDetail()).isEqualTo("Access Denied");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:forbidden"));
    }

    @Test
    @DisplayName("Handler #4: ResourceNotFoundException ánh xạ thành 404 not-found")
    void handleResourceNotFound_returnsProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("WorkOrder not found with id: a1b2c3d4");

        ProblemDetail problem = handler.handleResourceNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("WorkOrder not found with id: a1b2c3d4");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:not-found"));
    }

    @Test
    @DisplayName("Handler #5: IllegalStateException ánh xạ thành 422 invalid-state-transition")
    void handleIllegalStateTransition_returnsProblemDetail() {
        IllegalStateException ex = new IllegalStateException("Invalid state transition from OPEN to DONE");

        ProblemDetail problem = handler.handleIllegalStateTransition(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getDetail()).isEqualTo("Invalid state transition from OPEN to DONE");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:invalid-state-transition"));
    }

    @Test
    @DisplayName("Handler #6: Exception fallback ánh xạ thành 500 internal-error không lộ thông tin nhạy cảm")
    void handleUnexpected_returnsProblemDetail() {
        Exception ex = new RuntimeException("Sensitive database connection failure details");

        ProblemDetail problem = handler.handleUnexpected(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:internal-error"));
        // Xác nhận chi tiết ngoại lệ nhạy cảm không bị lộ ra bên ngoài
        assertThat(problem.getDetail()).doesNotContain("database connection failure");
    }
}
