package com.company.shared.csvvat.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Immutable record representing the overall result of an order calculation.
 * <p>
 * Clearly exposes the three essential financial figures:
 * <ul>
 *     <li><b>subtotal:</b> Tổng tiền trước thuế (Subtotal)</li>
 *     <li><b>totalVat:</b> Tổng tiền thuế VAT (Total VAT)</li>
 *     <li><b>grandTotal:</b> Tổng thanh toán cuối cùng (Grand Total = Subtotal + Total VAT)</li>
 * </ul>
 */
public record OrderCalculationResult(
    String orderId,
    List<CalculatedItem> items,
    BigDecimal subtotal,
    BigDecimal totalVat,
    BigDecimal grandTotal,
    BigDecimal totalQuantity,
    int totalItemsCount
) {

    public OrderCalculationResult {
        Objects.requireNonNull(items, "items must not be null");
        Objects.requireNonNull(subtotal, "subtotal must not be null");
        Objects.requireNonNull(totalVat, "totalVat must not be null");
        Objects.requireNonNull(grandTotal, "grandTotal must not be null");
        Objects.requireNonNull(totalQuantity, "totalQuantity must not be null");

        items = List.copyOf(items);

        if (totalItemsCount < 0) {
            throw new IllegalArgumentException("totalItemsCount must not be negative");
        }
        if (totalQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalQuantity must not be negative");
        }
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("subtotal must not be negative");
        }
        if (totalVat.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("totalVat must not be negative");
        }
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("grandTotal must not be negative");
        }
    }

    public BigDecimal getTotalBeforeTax() {
        return subtotal;
    }

    public BigDecimal getTotalVatAmount() {
        return totalVat;
    }

    public BigDecimal getFinalPayableAmount() {
        return grandTotal;
    }
}
