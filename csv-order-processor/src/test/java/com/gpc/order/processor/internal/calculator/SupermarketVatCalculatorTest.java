package com.gpc.order.processor.internal.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import com.gpc.order.processor.api.model.OrderItem;
import com.gpc.order.processor.api.model.OrderSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class SupermarketVatCalculatorTest {

  @Test
  @DisplayName("Kiểm tra làm tròn HALF_UP chuẩn siêu thị: 1234.565 -> 1234.57 và 1234.564 -> 1234.56")
  void testRoundMoneyHalfUp() {
    BigDecimal val1 = new BigDecimal("1234.565");
    BigDecimal val2 = new BigDecimal("1234.564");

    assertThat(SupermarketVatCalculator.roundMoney(val1)).isEqualByComparingTo("1234.57");
    assertThat(SupermarketVatCalculator.roundMoney(val2)).isEqualByComparingTo("1234.56");
  }

  @Test
  @DisplayName("Tính thuế VAT từng dòng và cộng dồn tổng đơn hàng không gây sai số tích lũy")
  void testCalculateOrderSummary() {
    OrderItem item1 = new OrderItem(
        1,
        List.of(),
        new BigDecimal("1"),
        new BigDecimal("12345.65"),
        new BigDecimal("12345.65"),
        new BigDecimal("10"),
        new BigDecimal("1234.57"),
        new BigDecimal("13580.22"));

    OrderItem item2 = new OrderItem(
        2,
        List.of(),
        new BigDecimal("1"),
        new BigDecimal("12345.64"),
        new BigDecimal("12345.64"),
        new BigDecimal("10"),
        new BigDecimal("1234.56"),
        new BigDecimal("13580.20"));

    OrderSummary summary = SupermarketVatCalculator.calculateOrderSummary(List.of(item1, item2));

    assertThat(summary.subtotal()).isEqualByComparingTo("24691.29");
    assertThat(summary.totalVat()).isEqualByComparingTo("2469.13");
    assertThat(summary.finalTotal()).isEqualByComparingTo("27160.42");
  }
}
