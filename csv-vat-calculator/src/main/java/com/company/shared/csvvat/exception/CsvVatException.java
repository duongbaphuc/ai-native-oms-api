package com.company.shared.csvvat.exception;

/**
 * Base unchecked exception for all errors occurring within the csv-vat-calculator library.
 */
public class CsvVatException extends RuntimeException {

    public CsvVatException(String message) {
        super(message);
    }

    public CsvVatException(String message, Throwable cause) {
        super(message, cause);
    }
}
