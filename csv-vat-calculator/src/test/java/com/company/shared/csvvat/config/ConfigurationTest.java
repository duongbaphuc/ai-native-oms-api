package com.company.shared.csvvat.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Kiểm thử Cấu hình - CalculatorConfig, CsvColumnMapping, LineTotalDiscrepancyStrategy")
class ConfigurationTest {

    @Test
    @DisplayName("CsvColumnMapping: Builder, isDynamic, hasLineTotalColumn, getLineTotalColumn")
    void csvColumnMapping_methods() {
        CsvColumnMapping defaultMapping = CsvColumnMapping.defaultMapping();
        assertThat(defaultMapping.isDynamic()).isTrue();
        assertThat(defaultMapping.hasLineTotalColumn()).isFalse();
        assertThat(defaultMapping.getLineTotalColumn()).isEmpty();

        CsvColumnMapping emptyMapping = new CsvColumnMapping(null, null, null, null, null);
        assertThat(emptyMapping.isDynamic()).isFalse();

        CsvColumnMapping custom = CsvColumnMapping.builder()
                .itemNameColumn("Item")
                .quantityColumn("Qty")
                .unitPriceColumn("Price")
                .vatPercentageColumn("Vat")
                .lineTotalColumn("Total")
                .build();

        assertThat(custom.hasLineTotalColumn()).isTrue();
        assertThat(custom.getLineTotalColumn()).contains("Total");
        assertThat(custom.isDynamic()).isTrue();

        CsvColumnMapping blankLineTotal = CsvColumnMapping.builder().lineTotalColumn("   ").build();
        assertThat(blankLineTotal.hasLineTotalColumn()).isFalse();
    }

    @Test
    @DisplayName("CalculatorConfig: Builder, Validation, Defaults")
    void calculatorConfig_validationsAndDefaults() {
        CalculatorConfig config = CalculatorConfig.defaultConfig();
        assertThat(config.currencyScale()).isEqualTo(CalculatorConfig.DEFAULT_SCALE);
        assertThat(config.roundingMode()).isEqualTo(CalculatorConfig.DEFAULT_ROUNDING_MODE);
        assertThat(config.discrepancyStrategy()).isEqualTo(CalculatorConfig.DEFAULT_STRATEGY);
        assertThat(config.discrepancyTolerance()).isEqualTo(CalculatorConfig.DEFAULT_TOLERANCE);
        assertThat(config.columnMapping()).isEqualTo(CsvColumnMapping.defaultMapping());
        assertThat(config.writeUtf8Bom()).isTrue();

        CalculatorConfig disabledBomConfig = CalculatorConfig.builder().writeUtf8Bom(false).build();
        assertThat(disabledBomConfig.writeUtf8Bom()).isFalse();

        // Negative scale
        assertThatThrownBy(() -> new CalculatorConfig(-1, RoundingMode.HALF_UP, LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH, BigDecimal.ONE, CsvColumnMapping.defaultMapping()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currencyScale must not be negative");

        // Negative tolerance
        assertThatThrownBy(() -> new CalculatorConfig(2, RoundingMode.HALF_UP, LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH, new BigDecimal("-0.01"), CsvColumnMapping.defaultMapping()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discrepancyTolerance must not be negative");

        // Null validations
        assertThatThrownBy(() -> new CalculatorConfig(2, null, LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH, BigDecimal.ONE, CsvColumnMapping.defaultMapping()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CalculatorConfig(2, RoundingMode.HALF_UP, null, BigDecimal.ONE, CsvColumnMapping.defaultMapping()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CalculatorConfig(2, RoundingMode.HALF_UP, LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH, null, CsvColumnMapping.defaultMapping()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CalculatorConfig(2, RoundingMode.HALF_UP, LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH, BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);

        // Builder with null columnMapping defaults to defaultMapping
        CalculatorConfig builtWithNullMapping = CalculatorConfig.builder().columnMapping(null).build();
        assertThat(builtWithNullMapping.columnMapping()).isEqualTo(CsvColumnMapping.defaultMapping());

        // 5-argument backward-compatible constructor
        CalculatorConfig config5 = new CalculatorConfig(
                0,
                RoundingMode.FLOOR,
                LineTotalDiscrepancyStrategy.ACCEPT_INPUT_TOTAL,
                BigDecimal.ZERO,
                CsvColumnMapping.defaultMapping()
        );
        assertThat(config5.currencyScale()).isEqualTo(0);
        assertThat(config5.roundingMode()).isEqualTo(RoundingMode.FLOOR);
        assertThat(config5.discrepancyStrategy()).isEqualTo(LineTotalDiscrepancyStrategy.ACCEPT_INPUT_TOTAL);
        assertThat(config5.discrepancyTolerance()).isEqualTo(BigDecimal.ZERO);
        assertThat(config5.writeUtf8Bom()).isTrue();
    }

    @Test
    @DisplayName("LineTotalDiscrepancyStrategy: Enum values check")
    void discrepancyStrategy_enumValues() {
        assertThat(LineTotalDiscrepancyStrategy.values()).containsExactly(
                LineTotalDiscrepancyStrategy.FAIL_ON_MISMATCH,
                LineTotalDiscrepancyStrategy.WARN_AND_RECALCULATE,
                LineTotalDiscrepancyStrategy.ACCEPT_INPUT_TOTAL
        );
    }
}
