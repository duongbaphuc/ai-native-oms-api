package com.gpc.order.processor.api;

import com.gpc.order.processor.internal.engine.DefaultCsvOrderProcessor;

/**
 * Giao diện công khai chính của thư viện để xử lý dữ liệu đơn hàng CSV.
 */
public interface CsvOrderProcessor extends OrderProcessor {

  /**
   * Tạo một thể hiện mặc định của CsvOrderProcessor.
   * Triển khai cụ thể (DefaultCsvOrderProcessor) được ẩn giấu hoàn toàn bên trong package internal.
   *
   * @return đối tượng triển khai mặc định của bộ xử lý đơn hàng CSV
   */
  static CsvOrderProcessor create() {
    return new DefaultCsvOrderProcessor();
  }
}
