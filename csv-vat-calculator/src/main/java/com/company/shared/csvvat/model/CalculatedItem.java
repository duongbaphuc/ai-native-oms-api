package com.company.shared.csvvat.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Immutable record representing an item enriched with computed monetary amounts (Subtotal, VAT, Total).
 *
 * @param item             the underlying original order item
 * @param lineSubtotal     the computed line amount before VAT
 * @param vatAmount        the computed VAT amount for this line
 * @param lineTotalWithVat the final total amount for this line including VAT (lineSubtotal + vatAmount)
 */
public record CalculatedItem(
    OrderItem item,
    BigDecimal lineSubtotal,
    BigDecimal vatAmount,
    BigDecimal lineTotalWithVat
) {

    public CalculatedItem {
        Objects.requireNonNull(item, "item must not be null");
        Objects.requireNonNull(lineSubtotal, "lineSubtotal must not be null");
        Objects.requireNonNull(vatAmount, "vatAmount must not be null");
        Objects.requireNonNull(lineTotalWithVat, "lineTotalWithVat must not be null");

        if (lineSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("lineSubtotal must not be negative");
        }
        if (vatAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("vatAmount must not be negative");
        }
        if (lineTotalWithVat.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("lineTotalWithVat must not be negative");
        }
    }

    public int rowNumber() {
        return item.rowNumber();
    }

    public String itemName() {
        return item.itemName();
    }

    public BigDecimal quantity() {
        return item.quantity();
    }

    public BigDecimal unitPrice() {
        return item.unitPrice();
    }

    public BigDecimal vatPercentage() {
        return item.vatPercentage();
    }
}
