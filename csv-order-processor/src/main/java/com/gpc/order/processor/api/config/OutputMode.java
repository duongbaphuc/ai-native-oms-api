package com.gpc.order.processor.api.config;

/**
 * Lựa chọn chế độ đầu ra khi xử lý file CSV đơn hàng.
 */
public enum OutputMode {
  /**
   * Chế độ Báo cáo: Trả về CSV kèm 3 dòng footer tổng cộng ở cuối file.
   */
  REPORT_MODE,

  /**
   * Chế độ Tích hợp: Trả về CSV thuần danh sách mặt hàng (đã thêm cột VAT) và đối tượng OrderSummary rời rạc.
   */
  DATA_MODE
}
