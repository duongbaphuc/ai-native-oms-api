package com.company.shared.csvvat.config;

import java.util.Optional;

/**
 * Immutable configuration for dynamic CSV column mapping.
 * <p>
 * Allows client projects to bind customized CSV headers to the domain data model,
 * preventing hardcoded column constraints.
 *
 * @param itemNameColumn      the custom header for the item name column
 * @param quantityColumn      the custom header for the quantity column
 * @param unitPriceColumn     the custom header for the unit price column
 * @param vatPercentageColumn the custom header for the VAT percentage column
 * @param lineTotalColumn     optional custom header for the pre-calculated input line total
 */
public record CsvColumnMapping(
    String itemNameColumn,
    String quantityColumn,
    String unitPriceColumn,
    String vatPercentageColumn,
    String lineTotalColumn
) {

    /**
     * Standard default column mapping.
     */
    public static CsvColumnMapping defaultMapping() {
        return new CsvColumnMapping("Item Name", "Quantity", "Unit Price", "VAT (%)", null);
    }

    /**
     * Returns a new {@link Builder} to construct custom column mappings.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Determines whether any explicit custom mapping was supplied by the caller.
     *
     * @return true if caller customized at least one column name, false otherwise
     */
    public boolean isDynamic() {
        return itemNameColumn != null || quantityColumn != null
                || unitPriceColumn != null || vatPercentageColumn != null
                || lineTotalColumn != null;
    }

    public boolean hasLineTotalColumn() {
        return lineTotalColumn != null && !lineTotalColumn.trim().isEmpty();
    }

    public Optional<String> getLineTotalColumn() {
        return Optional.ofNullable(lineTotalColumn);
    }

    /**
     * Fluent Builder for {@link CsvColumnMapping}.
     */
    public static class Builder {
        private String itemNameColumn;
        private String quantityColumn;
        private String unitPriceColumn;
        private String vatPercentageColumn;
        private String lineTotalColumn;

        public Builder itemNameColumn(String itemNameColumn) {
            this.itemNameColumn = itemNameColumn;
            return this;
        }

        public Builder quantityColumn(String quantityColumn) {
            this.quantityColumn = quantityColumn;
            return this;
        }

        public Builder unitPriceColumn(String unitPriceColumn) {
            this.unitPriceColumn = unitPriceColumn;
            return this;
        }

        public Builder vatPercentageColumn(String vatPercentageColumn) {
            this.vatPercentageColumn = vatPercentageColumn;
            return this;
        }

        public Builder lineTotalColumn(String lineTotalColumn) {
            this.lineTotalColumn = lineTotalColumn;
            return this;
        }

        public CsvColumnMapping build() {
            return new CsvColumnMapping(
                    itemNameColumn,
                    quantityColumn,
                    unitPriceColumn,
                    vatPercentageColumn,
                    lineTotalColumn
            );
        }
    }
}
