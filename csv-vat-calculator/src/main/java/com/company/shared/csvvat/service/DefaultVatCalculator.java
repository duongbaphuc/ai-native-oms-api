package com.company.shared.csvvat.service;

import com.company.shared.csvvat.config.CalculatorConfig;
import com.company.shared.csvvat.config.LineTotalDiscrepancyStrategy;
import com.company.shared.csvvat.exception.EmptyOrderException;
import com.company.shared.csvvat.exception.LineTotalDiscrepancyException;
import com.company.shared.csvvat.model.CalculatedItem;
import com.company.shared.csvvat.model.OrderCalculationResult;
import com.company.shared.csvvat.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Production-ready immutable, thread-safe implementation of {@link VatCalculator} (v2.0).
 * <p>
 * Supports:
 * <ul>
 *     <li>Configurable scale and rounding mode via {@link CalculatorConfig}.</li>
 *     <li>Cross-validation between calculated subtotal and input line total using {@link LineTotalDiscrepancyStrategy}.</li>
 *     <li>Strict validation preventing negative quantities.</li>
 *     <li>Consistent financial aggregation conforming to corporate accounting standards.</li>
 * </ul>
 */
public class DefaultVatCalculator implements VatCalculator {

    private static final Logger log = LoggerFactory.getLogger(DefaultVatCalculator.class);
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final CalculatorConfig config;

    /**
     * Constructs a calculator with default configuration: scale = 2, {@link RoundingMode#HALF_UP},
     * {@link LineTotalDiscrepancyStrategy#WARN_AND_RECALCULATE}, tolerance = 0.01.
     */
    public DefaultVatCalculator() {
        this(CalculatorConfig.defaultConfig());
    }

    /**
     * Constructs a calculator with an explicit {@link CalculatorConfig}.
     *
     * @param config the complete configuration specification
     */
    public DefaultVatCalculator(CalculatorConfig config) {
        this.config = Objects.requireNonNull(config, "CalculatorConfig must not be null");
    }

    /**
     * Backward-compatible constructor for customizing currency scale and rounding mode.
     *
     * @param currencyScale the number of decimal digits to retain
     * @param roundingMode  the financial rounding mode
     */
    public DefaultVatCalculator(int currencyScale, RoundingMode roundingMode) {
        this(CalculatorConfig.builder()
                .currencyScale(currencyScale)
                .roundingMode(roundingMode)
                .build());
    }

    @Override
    public CalculatedItem calculateItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("OrderItem must not be null");
        }

        // 1. Strict validation: Quantity cannot be negative or zero
        if (item.quantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative: " + item.quantity());
        }
        if (item.quantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        int scale = config.currencyScale();
        RoundingMode rounding = config.roundingMode();

        // 2. Compute calculated subtotal = Quantity * UnitPrice
        BigDecimal rawSubtotal = item.quantity().multiply(item.unitPrice());
        BigDecimal calculatedSubtotal = rawSubtotal.setScale(scale, rounding);

        // 3. Line Total Cross-Validation Strategy
        BigDecimal effectiveSubtotal = calculatedSubtotal;
        if (item.inputLineTotal() != null) {
            BigDecimal inputTotal = item.inputLineTotal().setScale(scale, rounding);
            BigDecimal difference = calculatedSubtotal.subtract(inputTotal).abs();

            if (difference.compareTo(config.discrepancyTolerance()) > 0) {
                switch (config.discrepancyStrategy()) {
                    case FAIL_ON_MISMATCH -> {
                        String colName = config.columnMapping().lineTotalColumn() != null
                                ? config.columnMapping().lineTotalColumn()
                                : "Line Total";
                        throw new LineTotalDiscrepancyException(
                                item.rowNumber(), colName, calculatedSubtotal, inputTotal, difference
                        );
                    }
                    case WARN_AND_RECALCULATE -> {
                        log.warn("Row {}: Input line total {} differs from calculated subtotal {} by {} (tolerance: {}). Recalculating using Quantity * UnitPrice.",
                                item.rowNumber(), inputTotal, calculatedSubtotal, difference, config.discrepancyTolerance());
                        effectiveSubtotal = calculatedSubtotal;
                    }
                    case ACCEPT_INPUT_TOTAL -> {
                        log.info("Row {}: Accepting input line total {} instead of calculated subtotal {} per ACCEPT_INPUT_TOTAL strategy.",
                                item.rowNumber(), inputTotal, calculatedSubtotal);
                        effectiveSubtotal = inputTotal;
                    }
                }
            }
        }

        // 4. Normalize VAT percentage and compute VAT Amount
        BigDecimal normalizedVat = item.vatPercentage().setScale(scale, rounding);
        BigDecimal vatAmount;
        if (normalizedVat.compareTo(BigDecimal.ZERO) == 0) {
            vatAmount = BigDecimal.ZERO.setScale(scale, rounding);
        } else {
            BigDecimal rawVat = effectiveSubtotal.multiply(normalizedVat);
            vatAmount = rawVat.divide(ONE_HUNDRED, scale, rounding);
        }

        // 5. Line Total With VAT = Effective Subtotal + Item VAT
        BigDecimal lineTotalWithVat = effectiveSubtotal.add(vatAmount).setScale(scale, rounding);

        return new CalculatedItem(item, effectiveSubtotal, vatAmount, lineTotalWithVat);
    }

    @Override
    public OrderCalculationResult calculateOrder(String orderId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new EmptyOrderException("Order does not contain any items to calculate");
        }

        int scale = config.currencyScale();
        RoundingMode rounding = config.roundingMode();

        List<CalculatedItem> calculatedItems = new ArrayList<>(items.size());
        BigDecimal totalSubtotal = BigDecimal.ZERO.setScale(scale, rounding);
        BigDecimal totalVat = BigDecimal.ZERO.setScale(scale, rounding);
        BigDecimal totalQuantity = BigDecimal.ZERO;

        for (OrderItem item : items) {
            CalculatedItem calculated = calculateItem(item);
            calculatedItems.add(calculated);

            totalSubtotal = totalSubtotal.add(calculated.lineSubtotal()).setScale(scale, rounding);
            totalVat = totalVat.add(calculated.vatAmount()).setScale(scale, rounding);
            totalQuantity = totalQuantity.add(calculated.quantity());
        }

        BigDecimal grandTotal = totalSubtotal.add(totalVat).setScale(scale, rounding);

        return new OrderCalculationResult(
                orderId,
                calculatedItems,
                totalSubtotal,
                totalVat,
                grandTotal,
                totalQuantity,
                calculatedItems.size()
        );
    }

    public CalculatorConfig getConfig() {
        return config;
    }

    public int getCurrencyScale() {
        return config.currencyScale();
    }

    public RoundingMode getRoundingMode() {
        return config.roundingMode();
    }
}
