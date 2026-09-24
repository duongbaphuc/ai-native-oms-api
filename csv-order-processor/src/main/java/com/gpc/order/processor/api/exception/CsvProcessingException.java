package com.gpc.order.processor.api.exception;

/**
 * Ngoại lệ chuyên biệt ném ra khi có bất kỳ dòng dữ liệu nào vi phạm định dạng
 * hoặc thiếu dữ liệu bắt buộc trong quá trình xử lý file CSV (Strict Validation).
 */
public class CsvProcessingException extends RuntimeException {

  private final int lineNumber;
  private final Object columnIdentifier;

  /**
   * Khởi tạo CsvProcessingException với thông tin dòng, cột và mô tả lỗi.
   *
   * @param lineNumber số thứ tự dòng gặp lỗi trong file CSV (1-indexed)
   * @param columnIdentifier tên cột hoặc chỉ số cột xảy ra lỗi
   * @param message chi tiết nguyên nhân lỗi
   */
  public CsvProcessingException(int lineNumber, Object columnIdentifier, String message) {
    super(String.format(
        "CSV Processing Error at line %d, column '%s': %s",
        lineNumber, columnIdentifier, message));
    this.lineNumber = lineNumber;
    this.columnIdentifier = columnIdentifier;
  }

  /**
   * Khởi tạo CsvProcessingException với thông tin dòng, cột, mô tả lỗi và ngoại lệ nguyên nhân gốc.
   *
   * @param lineNumber số thứ tự dòng gặp lỗi trong file CSV (1-indexed)
   * @param columnIdentifier tên cột hoặc chỉ số cột xảy ra lỗi
   * @param message chi tiết nguyên nhân lỗi
   * @param cause ngoại lệ nguyên nhân gốc
   */
  public CsvProcessingException(
      int lineNumber, Object columnIdentifier, String message, Throwable cause) {
    super(String.format(
        "CSV Processing Error at line %d, column '%s': %s",
        lineNumber, columnIdentifier, message), cause);
    this.lineNumber = lineNumber;
    this.columnIdentifier = columnIdentifier;
  }

  /**
   * Lấy số thứ tự dòng xảy ra lỗi trong file CSV.
   *
   * @return số dòng 1-indexed
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Lấy định danh cột xảy ra lỗi (tên chuỗi String hoặc chỉ số cột Integer).
   *
   * @return định danh cột gây lỗi
   */
  public Object getColumnIdentifier() {
    return columnIdentifier;
  }
}
