package com.gpc.order.processor.internal.validator;

import com.gpc.order.processor.api.exception.CsvProcessingException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Bộ kiểm tra tính toàn vẹn nghiêm ngặt của từng dòng dữ liệu (Strict Validation).
 * Dừng tiến trình ngay lập tức và ném CsvProcessingException khi phát hiện vi phạm.
 */
public final class StrictRowValidator {

  private StrictRowValidator() {
    // Utility class
  }

  public static String extractCellValue(
      int lineNumber, List<String> row, int columnIndex, Object identifier) {
    if (columnIndex >= row.size()) {
      throw new CsvProcessingException(
          lineNumber,
          identifier,
          "Row does not contain enough columns (expected column at index "
              + columnIndex
              + ", but row only has "
              + row.size()
              + " columns)");
    }
    String val = row.get(columnIndex);
    if (val == null || val.trim().isEmpty()) {
      throw new CsvProcessingException(
          lineNumber, identifier, "Required cell value is empty or missing");
    }
    return val.trim();
  }

  public static BigDecimal parseBigDecimal(
      int lineNumber, Object identifier, String rawValue, String fieldDescription) {
    try {
      return new BigDecimal(rawValue);
    } catch (NumberFormatException e) {
      throw new CsvProcessingException(
          lineNumber,
          identifier,
          "Invalid numeric format for " + fieldDescription + ": '" + rawValue + "'",
          e);
    }
  }
}
