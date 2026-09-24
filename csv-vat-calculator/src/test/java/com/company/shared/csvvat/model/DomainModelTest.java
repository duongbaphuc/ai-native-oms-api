package com.company.shared.csvvat.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Kiểm thử Domain Models - OrderItem, CalculatedItem, OrderCalculationResult")
class DomainModelTest {

    @Test
    @DisplayName("OrderItem: Khởi tạo đầy đủ và kiểm tra validation")
    void orderItem_validations() {
        OrderItem item = new OrderItem(1, "SP A", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("10"));
        assertThat(item.getInputLineTotal()).contains(new BigDecimal("10"));

        OrderItem item5Args = new OrderItem(2, "SP B", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);
        assertThat(item5Args.inputLineTotal()).isNull();
        assertThat(item5Args.getInputLineTotal()).isEmpty();

        // Null validations
        assertThatThrownBy(() -> new OrderItem(1, null, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderItem(1, "SP", null, BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ONE, null, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ONE, BigDecimal.TEN, null))
                .isInstanceOf(NullPointerException.class);

        // Blank item name
        assertThatThrownBy(() -> new OrderItem(1, "  ", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemName must not be blank");

        // Negative values
        assertThatThrownBy(() -> new OrderItem(1, "SP", new BigDecimal("-1"), BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ONE, new BigDecimal("-5"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderItem(1, "SP", BigDecimal.ONE, BigDecimal.TEN, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("CalculatedItem: Kiểm tra delegation và validation")
    void calculatedItem_delegationAndValidation() {
        OrderItem raw = new OrderItem(3, "Bánh bao", new BigDecimal("2"), new BigDecimal("15000"), new BigDecimal("8"));
        CalculatedItem calc = new CalculatedItem(raw, new BigDecimal("30000"), new BigDecimal("2400"), new BigDecimal("32400"));

        assertThat(calc.rowNumber()).isEqualTo(3);
        assertThat(calc.itemName()).isEqualTo("Bánh bao");
        assertThat(calc.quantity()).isEqualByComparingTo("2");
        assertThat(calc.unitPrice()).isEqualByComparingTo("15000");
        assertThat(calc.vatPercentage()).isEqualByComparingTo("8");
        assertThat(calc.lineSubtotal()).isEqualByComparingTo("30000");
        assertThat(calc.vatAmount()).isEqualByComparingTo("2400");
        assertThat(calc.lineTotalWithVat()).isEqualByComparingTo("32400");

        // Null checks
        assertThatThrownBy(() -> new CalculatedItem(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CalculatedItem(raw, null, BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CalculatedItem(raw, BigDecimal.ZERO, null, BigDecimal.ZERO))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CalculatedItem(raw, BigDecimal.ZERO, BigDecimal.ZERO, null))
                .isInstanceOf(NullPointerException.class);

        // Negative checks
        assertThatThrownBy(() -> new CalculatedItem(raw, new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CalculatedItem(raw, BigDecimal.ZERO, new BigDecimal("-1"), BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CalculatedItem(raw, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("OrderCalculationResult: Getters và validation")
    void orderCalculationResult_gettersAndValidation() {
        OrderItem raw = new OrderItem(1, "Item", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO);
        CalculatedItem calc = new CalculatedItem(raw, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN);

        OrderCalculationResult result = new OrderCalculationResult(
                "ORD-999",
                List.of(calc),
                new BigDecimal("100"),
                new BigDecimal("10"),
                new BigDecimal("110"),
                BigDecimal.ONE,
                1
        );

        assertThat(result.orderId()).isEqualTo("ORD-999");
        assertThat(result.getTotalBeforeTax()).isEqualByComparingTo("100");
        assertThat(result.getTotalVatAmount()).isEqualByComparingTo("10");
        assertThat(result.getFinalPayableAmount()).isEqualByComparingTo("110");
        assertThat(result.items()).hasSize(1);

        // Null checks
        assertThatThrownBy(() -> new OrderCalculationResult("ID", null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), BigDecimal.ZERO, null, BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), BigDecimal.ZERO, BigDecimal.ZERO, null, BigDecimal.ZERO, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, 0))
                .isInstanceOf(NullPointerException.class);

        // Negative checks
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-1"), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), BigDecimal.ZERO, new BigDecimal("-1"), BigDecimal.ZERO, BigDecimal.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderCalculationResult("ID", List.of(), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("-1"), BigDecimal.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
