package com.company.shared.csvvat.exception;

import java.math.BigDecimal;

/**
 * Exception thrown when there is an unpermitted discrepancy between the calculated line subtotal
 * (Quantity * Unit Price) and the input line total provided in the CSV.
 */
public class LineTotalDiscrepancyException extends CsvRowValidationException {

    private final BigDecimal calculatedTotal;
    private final BigDecimal inputTotal;
    private final BigDecimal difference;

    public LineTotalDiscrepancyException(int rowNumber, String columnName, BigDecimal calculatedTotal, BigDecimal inputTotal, BigDecimal difference) {
        super(rowNumber, columnName, inputTotal != null ? inputTotal.toPlainString() : "null",
                String.format("Calculated subtotal %s differs from input total %s by %s, exceeding allowed tolerance",
                        calculatedTotal, inputTotal, difference));
        this.calculatedTotal = calculatedTotal;
        this.inputTotal = inputTotal;
        this.difference = difference;
    }

    public BigDecimal getCalculatedTotal() {
        return calculatedTotal;
    }

    public BigDecimal getInputTotal() {
        return inputTotal;
    }

    public BigDecimal getDifference() {
        return difference;
    }
}
