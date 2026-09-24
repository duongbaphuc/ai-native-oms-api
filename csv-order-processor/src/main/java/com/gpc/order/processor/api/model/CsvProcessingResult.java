package com.gpc.order.processor.api.model;

import com.gpc.order.processor.api.config.OutputMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Kết quả xử lý file CSV đơn hàng.
 * Đóng gói toàn bộ kết quả chuỗi CSV đầu ra, bảng tổng hợp và danh sách mặt hàng trong bộ nhớ.
 *
 * @param outputCsvContent Nội dung file CSV kết quả (theo REPORT_MODE có footer, hoặc DATA_MODE thuần mặt hàng).
 * @param summary          Đối tượng chứa 3 giá trị tổng rời rạc (Tổng trước thuế, Tổng VAT, Tổng thanh toán).
 * @param lineItems        Danh sách các mặt hàng chi tiết đã được tính toán (bất biến).
 * @param outputMode       Chế độ đầu ra đã áp dụng.
 */
public record CsvProcessingResult(
    String outputCsvContent,
    OrderSummary summary,
    List<OrderItem> lineItems,
    OutputMode outputMode) {

  public CsvProcessingResult {
    Objects.requireNonNull(outputCsvContent, "outputCsvContent must not be null");
    Objects.requireNonNull(summary, "summary must not be null");
    Objects.requireNonNull(outputMode, "outputMode must not be null");

    // Defensive copy để đảm bảo tính bất biến tuyệt đối
    lineItems = lineItems == null
        ? List.of()
        : Collections.unmodifiableList(new ArrayList<>(lineItems));
  }

  /**
   * Chuyển đổi kết quả xử lý thành đối tượng Aggregate Root Order hoàn chỉnh.
   *
   * @param orderId mã định danh đơn hàng
   * @return đối tượng Order chứa danh sách mặt hàng và tóm tắt tài chính
   */
  public Order toOrder(String orderId) {
    return new Order(orderId, lineItems, summary);
  }
}
