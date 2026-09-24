package com.gpc.order.processor.internal.calculator;

import com.gpc.order.processor.api.model.OrderItem;
import com.gpc.order.processor.api.model.OrderSummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Bộ xử lý tính toán thuế VAT và các giá trị tổng đơn hàng theo Chuẩn Siêu Thị:
 * - Quy tắc làm tròn: RoundingMode.HALF_UP.
 * - Số chữ số thập phân: Cố định 2 chữ số (scale = 2).
 * - Tránh sai số lũy kế: Các số tổng (Total) luôn là tổng của các giá trị đã được làm tròn ở từng dòng chi tiết.
 */
public final class SupermarketVatCalculator {

  public static final int MONEY_SCALE = 2;
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private SupermarketVatCalculator() {
    // Utility class
  }

  public static BigDecimal roundMoney(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
    }
    return value.setScale(MONEY_SCALE, ROUNDING_MODE);
  }

  public static BigDecimal calculateLineTotal(BigDecimal quantity, BigDecimal unitPrice) {
    Objects.requireNonNull(quantity, "quantity must not be null");
    Objects.requireNonNull(unitPrice, "unitPrice must not be null");
    return roundMoney(quantity.multiply(unitPrice));
  }

  public static BigDecimal calculateVatAmount(BigDecimal lineTotal, BigDecimal vatRate) {
    Objects.requireNonNull(lineTotal, "lineTotal must not be null");
    Objects.requireNonNull(vatRate, "vatRate must not be null");

    BigDecimal rawVat = lineTotal.multiply(vatRate).divide(ONE_HUNDRED, 8, RoundingMode.HALF_UP);
    return roundMoney(rawVat);
  }

  public static OrderSummary calculateOrderSummary(List<OrderItem> items) {
    if (items == null || items.isEmpty()) {
      return OrderSummary.zero();
    }

    BigDecimal subtotal = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
    BigDecimal totalVat = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

    for (OrderItem item : items) {
      if (item.lineTotal() != null) {
        subtotal = subtotal.add(item.lineTotal());
      }
      if (item.vatAmount() != null) {
        totalVat = totalVat.add(item.vatAmount());
      }
    }

    BigDecimal finalTotal = subtotal.add(totalVat).setScale(MONEY_SCALE, ROUNDING_MODE);
    return new OrderSummary(subtotal, totalVat, finalTotal);
  }
}
