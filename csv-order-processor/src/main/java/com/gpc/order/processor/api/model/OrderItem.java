package com.gpc.order.processor.api.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

  public OrderItem {
    if (lineNumber < 1) {
      throw new IllegalArgumentException("lineNumber must be positive (>= 1): " + lineNumber);
    }

    // Defensive copy để đảm bảo tính bất biến tuyệt đối
    rawValues = rawValues == null
        ? List.of()
        : Collections.unmodifiableList(new ArrayList<>(rawValues));

    if (unitPrice != null) {
      unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }
    if (lineTotal != null) {
      lineTotal = lineTotal.setScale(2, RoundingMode.HALF_UP);
    }
    if (vatAmount != null) {
      vatAmount = vatAmount.setScale(2, RoundingMode.HALF_UP);
    }
    if (lineTotalWithVat != null) {
      lineTotalWithVat = lineTotalWithVat.setScale(2, RoundingMode.HALF_UP);
    }
  }
}
