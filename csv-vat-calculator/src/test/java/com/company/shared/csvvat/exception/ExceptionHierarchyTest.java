package com.company.shared.csvvat.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Kiểm thử Cây phân cấp ngoại lệ (Exception Hierarchy)")
class ExceptionHierarchyTest {

    @Test
    @DisplayName("Khởi tạo bộ test ExceptionHierarchyTest")
    void contextLoads() {
        assertThat(new ExceptionHierarchyTest()).isNotNull();
    }

    @Test
    @DisplayName("CsvVatException: Kiểm tra message và cause")
    void csvVatException_constructors() {
        CsvVatException ex1 = new CsvVatException("Error occurred");
        assertThat(ex1.getMessage()).isEqualTo("Error occurred");

        Throwable cause = new RuntimeException("Root cause");
        CsvVatException ex2 = new CsvVatException("Wrapped error", cause);
        assertThat(ex2.getMessage()).isEqualTo("Wrapped error");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("InvalidCsvFormatException: Kiểm tra message và cause")
    void invalidCsvFormatException_constructors() {
        InvalidCsvFormatException ex1 = new InvalidCsvFormatException("Bad format");
        assertThat(ex1.getMessage()).isEqualTo("Bad format");

        Throwable cause = new IllegalArgumentException("Header missing");
        InvalidCsvFormatException ex2 = new InvalidCsvFormatException("Bad header", cause);
        assertThat(ex2.getMessage()).isEqualTo("Bad header");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("EmptyOrderException: Kiểm tra message và cause")
    void emptyOrderException_constructors() {
        EmptyOrderException ex1 = new EmptyOrderException("Empty items");
        assertThat(ex1.getMessage()).isEqualTo("Empty items");

        Throwable cause = new IllegalStateException("No records");
        EmptyOrderException ex2 = new EmptyOrderException("Empty", cause);
        assertThat(ex2.getMessage()).isEqualTo("Empty");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("CsvRowValidationException: Kiểm tra getters")
    void csvRowValidationException_getters() {
        CsvRowValidationException ex = new CsvRowValidationException(5, "Quantity", "-2", "Quantity cannot be negative");
        assertThat(ex.getRowNumber()).isEqualTo(5);
        assertThat(ex.getColumnName()).isEqualTo("Quantity");
        assertThat(ex.getInvalidValue()).isEqualTo("-2");
        assertThat(ex.getReason()).isEqualTo("Quantity cannot be negative");

        CsvRowValidationException nullCol = new CsvRowValidationException(5, null, "-2", "Reason");
        assertThat(nullCol.getColumnName()).isNull();
    }

    @Test
    @DisplayName("LineTotalDiscrepancyException: Kiểm tra getters")
    void lineTotalDiscrepancyException_getters() {
        LineTotalDiscrepancyException ex = new LineTotalDiscrepancyException(
                3, "Line Total", new BigDecimal("100"), new BigDecimal("120"), new BigDecimal("20")
        );
        assertThat(ex.getRowNumber()).isEqualTo(3);
        assertThat(ex.getColumnName()).isEqualTo("Line Total");
        assertThat(ex.getCalculatedTotal()).isEqualByComparingTo("100");
        assertThat(ex.getInputTotal()).isEqualByComparingTo("120");
        assertThat(ex.getDifference()).isEqualByComparingTo("20");

        LineTotalDiscrepancyException nullInputEx = new LineTotalDiscrepancyException(
                4, "Line Total", new BigDecimal("100"), null, new BigDecimal("100")
        );
        assertThat(nullInputEx.getInputTotal()).isNull();
        assertThat(nullInputEx.getInvalidValue()).isEqualTo("null");
    }
}
