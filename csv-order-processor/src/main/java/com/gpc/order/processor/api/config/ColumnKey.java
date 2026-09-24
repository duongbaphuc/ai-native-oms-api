package com.gpc.order.processor.api.config;

/**
 * Định danh các cột dữ liệu tiêu chuẩn cần thiết để xử lý và tính toán đơn hàng.
 */
public enum ColumnKey {
  /**
   * Cột Số lượng (bắt buộc nếu không có cột TOTAL sẵn).
   */
  QUANTITY,

  /**
   * Cột Đơn giá (bắt buộc nếu không có cột TOTAL sẵn).
   */
  UNIT_PRICE,

  /**
   * Cột Số tổng / Thành tiền dòng chi tiết (tùy chọn: nếu có thì ưu tiên dùng, nếu không sẽ lấy QUANTITY * UNIT_PRICE).
   */
  TOTAL,

  /**
   * Cột phần trăm thuế VAT (bắt buộc, ví dụ: 5, 8, 10 hoặc 10%).
   */
  VAT_RATE
}
