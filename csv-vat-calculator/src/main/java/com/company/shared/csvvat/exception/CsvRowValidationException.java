package com.company.shared.csvvat.exception;

/**
 * Exception thrown when a specific row within the CSV contains invalid, malformed, or out-of-range data.
 */
public class CsvRowValidationException extends CsvVatException {

    private final int rowNumber;
    private final String columnName;
    private final String invalidValue;
    private final String reason;

    public CsvRowValidationException(int rowNumber, String columnName, String invalidValue, String reason) {
        super(String.format("Validation failed at row %d, column '%s' [value: '%s']: %s",
                rowNumber, columnName != null ? columnName : "UNKNOWN", invalidValue, reason));
        this.rowNumber = rowNumber;
        this.columnName = columnName;
        this.invalidValue = invalidValue;
        this.reason = reason;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getInvalidValue() {
        return invalidValue;
    }

    public String getReason() {
        return reason;
    }
}
