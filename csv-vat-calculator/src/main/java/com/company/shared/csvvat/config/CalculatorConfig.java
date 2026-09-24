package com.company.shared.csvvat.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable configuration encapsulating all calculation policies, monetary precision settings,
 * cross-validation strategies, and dynamic CSV mappings.
 *
 * @param currencyScale        the number of decimal places for currency rounding (e.g. 2 for USD, 0 for VND)
 * @param roundingMode         the {@link RoundingMode} applied to all monetary arithmetic
 * @param discrepancyStrategy  the strategy for handling discrepancies with the input line total
 * @param discrepancyTolerance the maximum permissible monetary difference before a discrepancy is triggered
 * @param columnMapping        the dynamic column mapping specification
 * @param writeUtf8Bom         whether to write UTF-8 BOM prefix on binary stream/file outputs for Excel compatibility
 */
public record CalculatorConfig(
    int currencyScale,
    RoundingMode roundingMode,
    LineTotalDiscrepancyStrategy discrepancyStrategy,
    BigDecimal discrepancyTolerance,
    CsvColumnMapping columnMapping,
    boolean writeUtf8Bom
) {

    public static final int DEFAULT_SCALE = 2;
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final LineTotalDiscrepancyStrategy DEFAULT_STRATEGY = LineTotalDiscrepancyStrategy.WARN_AND_RECALCULATE;
    public static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("0.01");
    public static final boolean DEFAULT_WRITE_UTF8_BOM = true;

    /**
     * Backward-compatible constructor defaulting {@code writeUtf8Bom} to {@code true}.
     */
    public CalculatorConfig(
            int currencyScale,
            RoundingMode roundingMode,
            LineTotalDiscrepancyStrategy discrepancyStrategy,
            BigDecimal discrepancyTolerance,
            CsvColumnMapping columnMapping
    ) {
        this(currencyScale, roundingMode, discrepancyStrategy, discrepancyTolerance, columnMapping, DEFAULT_WRITE_UTF8_BOM);
    }

    /**
     * Compact constructor validating constraints.
     */
    public CalculatorConfig {
        if (currencyScale < 0) {
            throw new IllegalArgumentException("currencyScale must not be negative");
        }
        Objects.requireNonNull(roundingMode, "roundingMode must not be null");
        Objects.requireNonNull(discrepancyStrategy, "discrepancyStrategy must not be null");
        Objects.requireNonNull(discrepancyTolerance, "discrepancyTolerance must not be null");
        Objects.requireNonNull(columnMapping, "columnMapping must not be null");

        if (discrepancyTolerance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("discrepancyTolerance must not be negative");
        }
    }

    /**
     * Standard production default configuration.
     */
    public static CalculatorConfig defaultConfig() {
        return new CalculatorConfig(
                DEFAULT_SCALE,
                DEFAULT_ROUNDING_MODE,
                DEFAULT_STRATEGY,
                DEFAULT_TOLERANCE,
                CsvColumnMapping.defaultMapping(),
                DEFAULT_WRITE_UTF8_BOM
        );
    }

    /**
     * Creates a new fluent {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent Builder for {@link CalculatorConfig}.
     */
    public static class Builder {
        private int currencyScale = DEFAULT_SCALE;
        private RoundingMode roundingMode = DEFAULT_ROUNDING_MODE;
        private LineTotalDiscrepancyStrategy discrepancyStrategy = DEFAULT_STRATEGY;
        private BigDecimal discrepancyTolerance = DEFAULT_TOLERANCE;
        private CsvColumnMapping columnMapping = CsvColumnMapping.defaultMapping();
        private boolean writeUtf8Bom = DEFAULT_WRITE_UTF8_BOM;

        public Builder currencyScale(int currencyScale) {
            this.currencyScale = currencyScale;
            return this;
        }

        public Builder roundingMode(RoundingMode roundingMode) {
            this.roundingMode = roundingMode;
            return this;
        }

        public Builder discrepancyStrategy(LineTotalDiscrepancyStrategy discrepancyStrategy) {
            this.discrepancyStrategy = discrepancyStrategy;
            return this;
        }

        public Builder discrepancyTolerance(BigDecimal discrepancyTolerance) {
            this.discrepancyTolerance = discrepancyTolerance;
            return this;
        }

        public Builder columnMapping(CsvColumnMapping columnMapping) {
            this.columnMapping = columnMapping;
            return this;
        }

        public Builder writeUtf8Bom(boolean writeUtf8Bom) {
            this.writeUtf8Bom = writeUtf8Bom;
            return this;
        }

        public CalculatorConfig build() {
            return new CalculatorConfig(
                    currencyScale,
                    roundingMode,
                    discrepancyStrategy,
                    discrepancyTolerance,
                    columnMapping != null ? columnMapping : CsvColumnMapping.defaultMapping(),
                    writeUtf8Bom
            );
        }
    }
}
