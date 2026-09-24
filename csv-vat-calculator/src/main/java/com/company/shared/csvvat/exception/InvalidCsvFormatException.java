package com.company.shared.csvvat.exception;

/**
 * Exception thrown when the CSV structure, delimiter, or mandatory headers are invalid or missing.
 */
public class InvalidCsvFormatException extends CsvVatException {

    public InvalidCsvFormatException(String message) {
        super(message);
    }

    public InvalidCsvFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
