package com.company.shared.csvvat.service;

import com.company.shared.csvvat.exception.EmptyOrderException;
import com.company.shared.csvvat.model.CalculatedItem;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.model.OrderItem;

import java.util.List;

/**
 * Core contract for performing deterministic VAT and monetary calculations.
 */
public interface VatCalculator {

    CalculatedItem calculateItem(OrderItem item);

    default OrderCalculationResult calculateOrder(List<OrderItem> items) {
        return calculateOrder(null, items);
    }

    OrderCalculationResult calculateOrder(String orderId, List<OrderItem> items);
}
