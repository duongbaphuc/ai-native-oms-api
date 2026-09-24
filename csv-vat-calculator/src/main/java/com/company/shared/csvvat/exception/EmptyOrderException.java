package com.company.shared.csvvat.exception;

/**
 * Exception thrown when the CSV file contains headers but no valid order item rows.
 */
public class EmptyOrderException extends CsvVatException {

    public EmptyOrderException(String message) {
        super(message);
    }

    public EmptyOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
