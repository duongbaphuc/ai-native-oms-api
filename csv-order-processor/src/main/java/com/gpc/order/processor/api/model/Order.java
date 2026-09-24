package com.gpc.order.processor.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate Root đại diện cho 1 Đơn hàng hoàn chỉnh trong bộ nhớ.
 * Tuân thủ triệt để tính Bất biến (Immutable) thông qua Defensive Copying.
 *
 * @param orderId Mã định danh duy nhất của đơn hàng.
 * @param items   Danh sách các mặt hàng chi tiết (bất biến).
 * @param summary Chỉ số tổng kết tài chính của cả đơn hàng.
 */
public record Order(
    String orderId,
    List<OrderItem> items,
    OrderSummary summary) {

  public Order {
    Objects.requireNonNull(orderId, "orderId must not be null");
    if (orderId.isBlank()) {
      throw new IllegalArgumentException("orderId must not be blank");
    }
    Objects.requireNonNull(items, "items must not be null");
    Objects.requireNonNull(summary, "summary must not be null");

    // Defensive copy để đảm bảo tính bất biến tuyệt đối
    items = Collections.unmodifiableList(new ArrayList<>(items));
  }
}
