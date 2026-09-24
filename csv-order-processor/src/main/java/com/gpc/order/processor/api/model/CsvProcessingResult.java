package com.gpc.order.processor.api.model;

import com.gpc.order.processor.api.config.OutputMode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Kết quả xử lý file CSV đơn hàng.
 * Đóng gói toàn bộ kết quả chuỗi CSV đầu ra, bảng tổng hợp và danh sách mặt hàng trong bộ nhớ.
 * Hỗ trợ linh hoạt 2 hình thức trích xuất:
 * 1. Trích xuất cấu trúc dữ liệu miền (Structured Data): summary, lineItems, toOrder.
 * 2. Trích xuất mảng byte (Byte Array): getCsvBytes() để gửi trực tiếp qua mạng hoặc lưu file.
 *
 * @param outputCsvContent Nội dung file CSV kết quả (theo REPORT_MODE có footer, hoặc DATA_MODE thuần mặt hàng).
 * @param summary Đối tượng chứa 3 giá trị tổng rời rạc (Tổng trước thuế, Tổng VAT, Tổng thanh toán).
 * @param lineItems Danh sách các mặt hàng chi tiết đã được tính toán (bất biến).
 * @param outputMode Chế độ đầu ra đã áp dụng.
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
   * Lấy nội dung file CSV kết quả dưới dạng mảng byte (mặc định bảng mã UTF-8).
   * Phù hợp để ghi trực tiếp xuống OutputStream hoặc trả về file download trong HTTP response.
   *
   * @return mảng byte của file CSV kết quả
   */
  public byte[] getCsvBytes() {
    return outputCsvContent.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Lấy nội dung file CSV kết quả dưới dạng mảng byte theo bảng mã chỉ định.
   *
   * @param charset bảng mã ký tự mong muốn (vd: StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1)
   * @return mảng byte của file CSV kết quả
   */
  public byte[] getCsvBytes(Charset charset) {
    Objects.requireNonNull(charset, "charset must not be null");
    return outputCsvContent.getBytes(charset);
  }

  /**
   * Lấy luồng dữ liệu InputStream đọc nội dung file CSV kết quả.
   *
   * @return luồng InputStream của tệp CSV
   */
  public InputStream getCsvInputStream() {
    return new ByteArrayInputStream(getCsvBytes());
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
