// AI Provenance: generated from docs/00-api-rules.md §2, docs/00-security-rules.md §4
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

@DisplayName("GlobalExceptionHandler Direct Unit Tests (RFC 7807 Verification)")
class GlobalExceptionHandlerUnitTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Handler #1: MethodArgumentNotValidException maps 400 validation-error with non-null & null messages")
    void handleValidationErrors_withNonNullAndNullDefaultMessage() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "workOrderRequest");

        // Branch 1: error.getDefaultMessage() != null
        bindingResult.addError(new FieldError("workOrderRequest", "equipmentId", "equipmentId must not be blank"));

        // Branch 2: error.getDefaultMessage() == null (forces fallback "Invalid value")
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
    @DisplayName("Handler #2: HttpMessageNotReadableException maps 400 malformed-json with body param")
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
    @DisplayName("Handler #3: AccessDeniedException maps 403 forbidden")
    void handleAccessDenied_returnsProblemDetail() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ProblemDetail problem = handler.handleAccessDenied(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getDetail()).isEqualTo("Access Denied");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:forbidden"));
    }

    @Test
    @DisplayName("Handler #4: ResourceNotFoundException maps 404 not-found")
    void handleResourceNotFound_returnsProblemDetail() {
        ResourceNotFoundException ex = new ResourceNotFoundException("WorkOrder not found with id: a1b2c3d4");

        ProblemDetail problem = handler.handleResourceNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getDetail()).isEqualTo("WorkOrder not found with id: a1b2c3d4");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:not-found"));
    }

    @Test
    @DisplayName("Handler #5: IllegalStateException maps 422 invalid-state-transition")
    void handleIllegalStateTransition_returnsProblemDetail() {
        IllegalStateException ex = new IllegalStateException("Invalid state transition from OPEN to DONE");

        ProblemDetail problem = handler.handleIllegalStateTransition(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(problem.getDetail()).isEqualTo("Invalid state transition from OPEN to DONE");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:invalid-state-transition"));
    }

    @Test
    @DisplayName("Handler #6: Exception fallback maps 500 internal-error without leaking internal details")
    void handleUnexpected_returnsProblemDetail() {
        Exception ex = new RuntimeException("Sensitive database connection failure details");

        ProblemDetail problem = handler.handleUnexpected(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:internal-error"));
        // Assert sensitive exception details are not exposed in problem detail
        assertThat(problem.getDetail()).doesNotContain("database connection failure");
    }
}
