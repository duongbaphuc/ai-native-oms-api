package com.gpc.order.processor.internal.csv;

import java.util.List;

/**
 * Bộ tạo và định dạng nội dung CSV tuân thủ chuẩn RFC 4180 và Excel.
 */
public final class CsvWriter {

  private CsvWriter() {
    // Utility class
  }

  public static String formatRow(List<String> cells, char delimiter) {
    if (cells == null || cells.isEmpty()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < cells.size(); i++) {
      if (i > 0) {
        sb.append(delimiter);
      }
      sb.append(escapeField(cells.get(i), delimiter));
    }
    return sb.toString();
  }

  public static String escapeField(String field, char delimiter) {
    if (field == null) {
      return "";
    }

    boolean containsDelimiter = field.indexOf(delimiter) >= 0;
    boolean containsQuote = field.indexOf('"') >= 0;
    boolean containsNewline = field.indexOf('\n') >= 0 || field.indexOf('\r') >= 0;

    if (containsDelimiter || containsQuote || containsNewline) {
      String escaped = field.replace("\"", "\"\"");
      return "\"" + escaped + "\"";
    }
    return field;
  }
}
