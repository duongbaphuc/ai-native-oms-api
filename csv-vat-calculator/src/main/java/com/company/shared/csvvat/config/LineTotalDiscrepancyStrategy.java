package com.company.shared.csvvat.config;

/**
 * Strategy defining how the system behaves when the calculated line total (Quantity * Unit Price)
 * differs from the input line total provided in the CSV.
 */
public enum LineTotalDiscrepancyStrategy {

    /**
     * Strictly fail and throw {@link com.company.shared.csvvat.exception.LineTotalDiscrepancyException}
     * if the discrepancy exceeds the configured tolerance threshold.
     */
    FAIL_ON_MISMATCH,

    /**
     * Log a warning via SLF4J and automatically recompute using (Quantity * Unit Price) as the standard truth.
     * This is the default strategy.
     */
    WARN_AND_RECALCULATE,

    /**
     * Accept the input line total provided by the caller/source system as the effective subtotal.
     */
    ACCEPT_INPUT_TOTAL
}
