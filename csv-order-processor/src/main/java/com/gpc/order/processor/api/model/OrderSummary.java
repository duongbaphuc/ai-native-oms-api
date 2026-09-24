package com.gpc.order.processor.api.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Đối tượng chứa 3 chỉ số tổng cộng của cả đơn hàng:
 * 1. subtotal:   Tổng tiền hàng trước thuế = sum(lineTotal_i)
 * 2. totalVat:   Tổng tiền thuế VAT = sum(vatAmount_i)
 * 3. finalTotal: Tổng thanh toán cuối cùng = subtotal + totalVat
 *
 * <p>Tất cả các giá trị tiền tệ đều được làm tròn 2 chữ số thập phân theo chuẩn HALF_UP.</p>
 *
 * @param subtotal   Tổng tiền trước thuế (tổng các dòng chi tiết đã làm tròn).
 * @param totalVat   Tổng tiền thuế VAT (tổng các dòng tiền VAT chi tiết đã làm tròn).
 * @param finalTotal Tổng thanh toán cuối cùng (subtotal + totalVat).
 */
public record OrderSummary(
    BigDecimal subtotal,
    BigDecimal totalVat,
    BigDecimal finalTotal) {

  public static final int MONEY_SCALE = 2;
  public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

  public OrderSummary {
    Objects.requireNonNull(subtotal, "subtotal must not be null");
    Objects.requireNonNull(totalVat, "totalVat must not be null");
    Objects.requireNonNull(finalTotal, "finalTotal must not be null");

    subtotal = subtotal.setScale(MONEY_SCALE, ROUNDING_MODE);
    totalVat = totalVat.setScale(MONEY_SCALE, ROUNDING_MODE);
    finalTotal = finalTotal.setScale(MONEY_SCALE, ROUNDING_MODE);
  }

  /**
   * Khởi tạo đối tượng OrderSummary với tất cả các chỉ số bằng 0.00.
   *
   * @return đối tượng OrderSummary có giá trị mặc định là 0.00
   */
  public static OrderSummary zero() {
    BigDecimal zeroMoney = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
    return new OrderSummary(zeroMoney, zeroMoney, zeroMoney);
  }

  /**
   * Tính toán tổng đơn hàng từ danh sách các dòng mặt hàng đã xử lý.
   * Áp dụng nguyên tắc cộng dồn từ các giá trị dòng chi tiết đã làm tròn để tránh sai số tích lũy.
   *
   * @param items danh sách các mặt hàng chi tiết
   * @return đối tượng OrderSummary chứa số liệu tổng hợp chính xác
   */
  public static OrderSummary fromItems(List<OrderItem> items) {
    if (items == null || items.isEmpty()) {
      return zero();
    }

    BigDecimal calculatedSubtotal = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);
    BigDecimal calculatedTotalVat = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING_MODE);

    for (OrderItem item : items) {
      if (item.lineTotal() != null) {
        calculatedSubtotal = calculatedSubtotal.add(item.lineTotal());
      }
      if (item.vatAmount() != null) {
        calculatedTotalVat = calculatedTotalVat.add(item.vatAmount());
      }
    }

    BigDecimal calculatedFinalTotal = calculatedSubtotal.add(calculatedTotalVat)
        .setScale(MONEY_SCALE, ROUNDING_MODE);

    return new OrderSummary(calculatedSubtotal, calculatedTotalVat, calculatedFinalTotal);
  }
}
