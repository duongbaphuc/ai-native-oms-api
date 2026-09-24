package com.gpc.order.processor.api.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Đại diện cho 1 dòng mặt hàng trong tệp CSV đơn hàng sau khi phân tích và tính toán.
 * Mọi giá trị tiền tệ đều được làm tròn 2 chữ số thập phân theo chuẩn HALF_UP.
 *
 * @param lineNumber       Số thứ tự dòng trong file CSV gốc (1-indexed).
 * @param rawValues        Danh sách giá trị chuỗi gốc của các cột trong dòng (bất biến).
 * @param quantity         Số lượng mặt hàng.
 * @param unitPrice        Đơn giá mặt hàng (đã chuẩn hóa scale = 2).
 * @param lineTotal        Tổng tiền trước thuế của dòng (đã làm tròn HALF_UP 2 chữ số).
 * @param vatRate          Mức thuế suất VAT (phần trăm, vd: 10 nghĩa là 10%).
 * @param vatAmount        Số tiền VAT của dòng (đã làm tròn HALF_UP 2 chữ số).
 * @param lineTotalWithVat Tổng thanh toán dòng bao gồm thuế (lineTotal + vatAmount).
 */
public record OrderItem(
    int lineNumber,
    List<String> rawValues,
    BigDecimal quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    BigDecimal vatRate,
    BigDecimal vatAmount,
    BigDecimal lineTotalWithVat) {

  public static final int MONEY_SCALE = 2;
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  public OrderItem {
    if (lineNumber < 1) {
      throw new IllegalArgumentException("lineNumber must be positive (>= 1): " + lineNumber);
    }

    // Defensive copy để đảm bảo tính bất biến tuyệt đối
    rawValues = rawValues == null
        ? List.of()
        : Collections.unmodifiableList(new ArrayList<>(rawValues));

    if (unitPrice != null) {
      unitPrice = roundMoney(unitPrice);
    }
    if (lineTotal != null) {
      lineTotal = roundMoney(lineTotal);
    }
    if (vatAmount != null) {
      vatAmount = roundMoney(vatAmount);
    }
    if (lineTotalWithVat != null) {
      lineTotalWithVat = roundMoney(lineTotalWithVat);
    }
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

  public static OrderItem of(
      int lineNumber,
      List<String> rawValues,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal vatRate) {
    BigDecimal lineTotal = calculateLineTotal(quantity, unitPrice);
    BigDecimal vatAmount = calculateVatAmount(lineTotal, vatRate);
    BigDecimal lineTotalWithVat = lineTotal.add(vatAmount);

    return new OrderItem(
        lineNumber, rawValues, quantity, unitPrice, lineTotal, vatRate, vatAmount, lineTotalWithVat);
  }

  public static OrderItem ofWithLineTotal(
      int lineNumber,
      List<String> rawValues,
      BigDecimal rawLineTotal,
      BigDecimal vatRate) {
    BigDecimal lineTotal = roundMoney(rawLineTotal);
    BigDecimal vatAmount = calculateVatAmount(lineTotal, vatRate);
    BigDecimal lineTotalWithVat = lineTotal.add(vatAmount);

    return new OrderItem(
        lineNumber, rawValues, null, null, lineTotal, vatRate, vatAmount, lineTotalWithVat);
  }
}
