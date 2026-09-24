package com.gpc.order.processor.api.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Đối tượng chứa 3 chỉ số tổng cộng của cả đơn hàng.
 * Tất cả các giá trị tiền tệ đều được làm tròn 2 chữ số thập phân theo chuẩn HALF_UP.
 *
 * @param subtotal   Tổng tiền trước thuế (tổng các dòng chi tiết đã làm tròn).
 * @param totalVat   Tổng tiền thuế VAT (tổng các dòng tiền VAT chi tiết đã làm tròn).
 * @param finalTotal Tổng thanh toán cuối cùng (subtotal + totalVat).
 */
public record OrderSummary(
    BigDecimal subtotal,
    BigDecimal totalVat,
    BigDecimal finalTotal) {

  public OrderSummary {
    Objects.requireNonNull(subtotal, "subtotal must not be null");
    Objects.requireNonNull(totalVat, "totalVat must not be null");
    Objects.requireNonNull(finalTotal, "finalTotal must not be null");

    subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
    totalVat = totalVat.setScale(2, RoundingMode.HALF_UP);
    finalTotal = finalTotal.setScale(2, RoundingMode.HALF_UP);
  }

  /**
   * Khởi tạo đối tượng OrderSummary với tất cả các chỉ số bằng 0.00.
   *
   * @return đối tượng OrderSummary có giá trị mặc định là 0.00
   */
  public static OrderSummary zero() {
    return new OrderSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
  }
}
