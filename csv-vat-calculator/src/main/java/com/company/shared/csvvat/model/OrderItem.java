package com.company.shared.csvvat.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable record representing a single parsed raw item row from an order CSV file.
 *
 * @param rowNumber       the 1-based CSV line number where this item was found
 * @param itemName        the name or description of the item (must not be blank)
 * @param quantity        the purchased quantity (strictly positive, cannot be negative or zero)
 * @param unitPrice       the unit price of the item (must be non-negative)
 * @param vatPercentage   the applicable VAT percentage rate (must be non-negative, e.g. 10 for 10%)
 * @param inputLineTotal  optional pre-calculated line total supplied in the CSV for cross-validation
 */
public record OrderItem(
    int rowNumber,
    String itemName,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal vatPercentage,
    BigDecimal inputLineTotal
) {

    /**
     * Backward-compatible 5-argument constructor when no input line total column is present.
     */
    public OrderItem(int rowNumber, String itemName, BigDecimal quantity, BigDecimal unitPrice, BigDecimal vatPercentage) {
        this(rowNumber, itemName, quantity, unitPrice, vatPercentage, null);
    }

    /**
     * Compact constructor enforcing non-nullity and domain business invariants.
     */
    public OrderItem {
        Objects.requireNonNull(itemName, "itemName must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        Objects.requireNonNull(vatPercentage, "vatPercentage must not be null");

        if (itemName.isBlank()) {
            throw new IllegalArgumentException("itemName must not be blank");
        }
        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + quantity);
        }
        if (quantity.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("unitPrice must not be negative");
        }
        if (vatPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("vatPercentage must not be negative");
        }
    }

    public Optional<BigDecimal> getInputLineTotal() {
        return Optional.ofNullable(inputLineTotal);
    }
}
