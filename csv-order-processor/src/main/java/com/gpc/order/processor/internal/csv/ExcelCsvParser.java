package com.gpc.order.processor.internal.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Bộ phân tích cú pháp CSV tuân thủ chuẩn RFC 4180 và Excel:
 * - Hỗ trợ ký tự phân cách linh hoạt (dấu phẩy ',' hoặc chấm phẩy ';').
 * - Hỗ trợ dấu ngoặc kép bọc chuỗi (quoted field) chứa dấu phân cách, dấu nháy kép escaping ("") và xuống dòng.
 * - Hỗ trợ cả CRLF và LF.
 */
public final class ExcelCsvParser {

  private ExcelCsvParser() {
    // Utility class
  }

  /**
   * Phân tích nội dung chuỗi CSV thành danh sách các dòng và ô dữ liệu.
   *
   * @param csvContent nội dung văn bản CSV
   * @param delimiter ký tự phân cách các cột (ví dụ: ',' hoặc ';')
   * @return danh sách các dòng dữ liệu, mỗi dòng là danh sách các ô giá trị chuỗi
   */
  public static List<List<String>> parse(String csvContent, char delimiter) {
    if (csvContent == null || csvContent.isEmpty()) {
      return List.of();
    }

    List<List<String>> rows = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
      parseStream(reader, delimiter, rows);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to parse CSV content", e);
    }
    return rows;
  }

  private static void parseStream(
      BufferedReader reader,
      char delimiter,
      List<List<String>> rows)
      throws IOException {
    List<String> currentRow = new ArrayList<>();
    StringBuilder currentField = new StringBuilder();
    boolean inQuotes = false;
    int ch;

    while ((ch = reader.read()) != -1) {
      char c = (char) ch;
      if (inQuotes) {
        inQuotes = handleQuotedChar(reader, c, currentField);
      } else {
        inQuotes = handleUnquotedChar(reader, c, delimiter, currentField, currentRow, rows);
      }
    }

    flushRemaining(currentField, currentRow, rows);
  }

  private static boolean handleQuotedChar(
      BufferedReader reader,
      char c,
      StringBuilder currentField)
      throws IOException {
    if (c != '"') {
      currentField.append(c);
      return true;
    }

    reader.mark(1);
    int nextCh = reader.read();
    if (nextCh == '"') {
      currentField.append('"');
      return true;
    }

    if (nextCh != -1) {
      reader.reset();
    }
    return false;
  }

  private static boolean handleUnquotedChar(
      BufferedReader reader,
      char c,
      char delimiter,
      StringBuilder currentField,
      List<String> currentRow,
      List<List<String>> rows)
      throws IOException {
    if (c == '"') {
      return true;
    }

    if (c == delimiter) {
      commitField(currentField, currentRow);
      return false;
    }

    if (c == '\r' || c == '\n') {
      handleNewline(reader, c, currentField, currentRow, rows);
      return false;
    }

    currentField.append(c);
    return false;
  }

  private static void handleNewline(
      BufferedReader reader,
      char c,
      StringBuilder currentField,
      List<String> currentRow,
      List<List<String>> rows)
      throws IOException {
    if (c == '\r') {
      reader.mark(1);
      int nextCh = reader.read();
      if (nextCh != '\n' && nextCh != -1) {
        reader.reset();
      }
    }
    commitRow(currentField, currentRow, rows);
  }

  private static void commitField(StringBuilder currentField, List<String> currentRow) {
    currentRow.add(currentField.toString().trim());
    currentField.setLength(0);
  }

  private static void commitRow(
      StringBuilder currentField,
      List<String> currentRow,
      List<List<String>> rows) {
    commitField(currentField, currentRow);
    if (!isEmptyRow(currentRow)) {
      rows.add(new ArrayList<>(currentRow));
    }
    currentRow.clear();
  }

  private static void flushRemaining(
      StringBuilder currentField,
      List<String> currentRow,
      List<List<String>> rows) {
    if (currentField.length() > 0 || !currentRow.isEmpty()) {
      commitRow(currentField, currentRow, rows);
    }
  }

  private static boolean isEmptyRow(List<String> row) {
    if (row.isEmpty()) {
      return true;
    }
    for (String field : row) {
      if (!field.isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
