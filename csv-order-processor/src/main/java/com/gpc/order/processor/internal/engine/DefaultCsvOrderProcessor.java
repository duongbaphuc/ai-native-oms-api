package com.gpc.order.processor.internal.engine;

import com.gpc.order.processor.api.CsvOrderProcessor;
import com.gpc.order.processor.api.config.ColumnKey;
import com.gpc.order.processor.api.config.CsvConfig;
import com.gpc.order.processor.api.config.OutputMode;
import com.gpc.order.processor.api.exception.CsvProcessingException;
import com.gpc.order.processor.api.model.CsvProcessingResult;
import com.gpc.order.processor.api.model.OrderItem;
import com.gpc.order.processor.api.model.OrderSummary;
import com.gpc.order.processor.internal.calculator.SupermarketVatCalculator;
import com.gpc.order.processor.internal.csv.CsvWriter;
import com.gpc.order.processor.internal.csv.ExcelCsvParser;
import com.gpc.order.processor.internal.validator.StrictRowValidator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Động cơ điều phối và xử lý đơn hàng CSV (Internal Implementation).
 * Được che giấu hoàn toàn bên trong package internal và không được export qua module-info.
 */
public class DefaultCsvOrderProcessor implements CsvOrderProcessor {

  private static final String VAT_COLUMN_HEADER = "Tiền VAT";
  private static final String FOOTER_SUBTOTAL = "Tổng trước thuế";
  private static final String FOOTER_TOTAL_VAT = "Tổng VAT";
  private static final String FOOTER_FINAL_TOTAL = "Tổng thanh toán";

  @Override
  public CsvProcessingResult process(String csvContent, CsvConfig config) {
    Objects.requireNonNull(csvContent, "csvContent must not be null");
    Objects.requireNonNull(config, "config must not be null");

    List<List<String>> rows = ExcelCsvParser.parse(csvContent, config.delimiter());
    return processRows(rows, config);
  }

  @Override
  public CsvProcessingResult process(Reader reader, CsvConfig config) {
    Objects.requireNonNull(reader, "reader must not be null");
    Objects.requireNonNull(config, "config must not be null");

    try (BufferedReader bufferedReader = new BufferedReader(reader)) {
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        sb.append(line).append("\n");
      }
      return process(sb.toString(), config);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read CSV content from reader", e);
    }
  }

  @Override
  public CsvProcessingResult process(InputStream inputStream, CsvConfig config) {
    Objects.requireNonNull(inputStream, "inputStream must not be null");
    return process(new InputStreamReader(inputStream, StandardCharsets.UTF_8), config);
  }

  @Override
  public CsvProcessingResult process(Path filePath, CsvConfig config) {
    Objects.requireNonNull(filePath, "filePath must not be null");
    try {
      String content = Files.readString(filePath, StandardCharsets.UTF_8);
      return process(content, config);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read CSV file: " + filePath, e);
    }
  }

  private CsvProcessingResult processRows(List<List<String>> rows, CsvConfig config) {
    if (rows.isEmpty()) {
      OrderSummary emptySummary = OrderSummary.zero();
      return new CsvProcessingResult("", emptySummary, List.of(), config.outputMode());
    }

    List<String> headerRow = null;
    int dataStartRowIndex = 0;

    if (config.hasHeader()) {
      headerRow = rows.get(0);
      dataStartRowIndex = 1;
    }

    ResolvedMapping resolved = resolveMapping(config, headerRow);

    List<OrderItem> items = new ArrayList<>();
    List<List<String>> enrichedRows = new ArrayList<>();

    for (int i = dataStartRowIndex; i < rows.size(); i++) {
      List<String> rawRow = rows.get(i);
      int lineNumber = i + 1; // 1-indexed

      OrderItem item = parseAndCalculateLine(lineNumber, rawRow, resolved);
      items.add(item);

      List<String> enrichedRow = new ArrayList<>(rawRow);
      enrichedRow.add(item.vatAmount().toPlainString());
      enrichedRows.add(enrichedRow);
    }

    OrderSummary summary = SupermarketVatCalculator.calculateOrderSummary(items);
    String outputCsv = buildOutputCsv(headerRow, enrichedRows, summary, config);

    return new CsvProcessingResult(outputCsv, summary, items, config.outputMode());
  }

  private record ResolvedColumn(int columnIndex, Object identifier) {
  }

  private record ResolvedMapping(Map<ColumnKey, ResolvedColumn> keyToColumn) {
    public ResolvedColumn get(ColumnKey key) {
      return keyToColumn.get(key);
    }

    public boolean has(ColumnKey key) {
      return keyToColumn.containsKey(key);
    }
  }

  private ResolvedMapping resolveMapping(CsvConfig config, List<String> headerRow) {
    Map<Object, ColumnKey> normalized = normalizeMapping(config.columnMapping());
    Map<ColumnKey, ResolvedColumn> resolvedMap = new EnumMap<>(ColumnKey.class);

    for (Map.Entry<Object, ColumnKey> entry : normalized.entrySet()) {
      Object id = entry.getKey();
      ColumnKey key = entry.getValue();

      ResolvedColumn column = config.hasHeader()
          ? resolveHeaderColumn(id, headerRow)
          : resolveNoHeaderColumn(id);
      resolvedMap.put(key, column);
    }

    validateRequiredMappings(resolvedMap);
    return new ResolvedMapping(resolvedMap);
  }

  private ResolvedColumn resolveHeaderColumn(Object id, List<String> headerRow) {
    if (headerRow == null || headerRow.isEmpty()) {
      throw new IllegalArgumentException(
          "CSV header is declared in configuration but not present in CSV data");
    }

    if (id instanceof String colName) {
      return matchHeaderByName(colName, headerRow);
    }
    if (id instanceof Integer colIndex) {
      return matchHeaderByIndex(colIndex, headerRow);
    }
    throw new IllegalArgumentException(
        "Unsupported column identifier type: " + id.getClass().getName());
  }

  private ResolvedColumn matchHeaderByName(String colName, List<String> headerRow) {
    for (int i = 0; i < headerRow.size(); i++) {
      if (headerRow.get(i).trim().equalsIgnoreCase(colName.trim())) {
        return new ResolvedColumn(i, colName);
      }
    }
    throw new IllegalArgumentException(
        "Column '" + colName + "' declared in configuration was not found in CSV header: "
            + headerRow);
  }

  private ResolvedColumn matchHeaderByIndex(int colIndex, List<String> headerRow) {
    if (colIndex < 0 || colIndex >= headerRow.size()) {
      throw new IllegalArgumentException(
          "Column index " + colIndex + " declared in configuration is out of bounds for header (total columns: "
              + headerRow.size() + ")");
    }
    return new ResolvedColumn(colIndex, headerRow.get(colIndex));
  }

  private ResolvedColumn resolveNoHeaderColumn(Object id) {
    int colIndex;
    if (id instanceof Integer idx) {
      colIndex = idx;
    } else if (id instanceof String colName) {
      try {
        colIndex = Integer.parseInt(colName.trim());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Cannot map column by non-numeric String name '" + colName
                + "' when hasHeader is false. Please use 0-based Integer column index.");
      }
    } else {
      throw new IllegalArgumentException(
          "Unsupported column identifier type: " + id.getClass().getName());
    }

    if (colIndex < 0) {
      throw new IllegalArgumentException("Column index cannot be negative: " + colIndex);
    }
    return new ResolvedColumn(colIndex, colIndex);
  }

  private void validateRequiredMappings(Map<ColumnKey, ResolvedColumn> resolvedMap) {
    if (!resolvedMap.containsKey(ColumnKey.VAT_RATE)) {
      throw new IllegalArgumentException("Missing required column mapping for VAT_RATE (% VAT)");
    }

    boolean hasQuantityAndPrice =
        resolvedMap.containsKey(ColumnKey.QUANTITY) && resolvedMap.containsKey(ColumnKey.UNIT_PRICE);
    boolean hasTotal = resolvedMap.containsKey(ColumnKey.TOTAL);

    if (!hasQuantityAndPrice && !hasTotal) {
      throw new IllegalArgumentException(
          "Missing required column mapping: must map either (QUANTITY and UNIT_PRICE) or TOTAL");
    }
  }

  private Map<Object, ColumnKey> normalizeMapping(Map<Object, ColumnKey> rawMapping) {
    Map<Object, ColumnKey> result = new HashMap<>();
    for (Map.Entry<?, ?> entry : rawMapping.entrySet()) {
      if (entry.getKey() instanceof ColumnKey colKey) {
        result.put(entry.getValue(), colKey);
      } else if (entry.getValue() instanceof ColumnKey colKey) {
        result.put(entry.getKey(), colKey);
      } else {
        throw new IllegalArgumentException(
            "Invalid mapping entry: key=" + entry.getKey() + ", value=" + entry.getValue());
      }
    }
    return result;
  }

  private OrderItem parseAndCalculateLine(
      int lineNumber, List<String> row, ResolvedMapping resolved) {
    BigDecimal quantity = extractOptionalBigDecimal(lineNumber, row, resolved, ColumnKey.QUANTITY, "quantity");
    BigDecimal unitPrice = extractOptionalBigDecimal(lineNumber, row, resolved, ColumnKey.UNIT_PRICE, "unit price");
    BigDecimal lineTotal = resolveLineTotal(lineNumber, row, resolved, quantity, unitPrice);

    BigDecimal vatRate = extractVatRate(lineNumber, row, resolved);
    BigDecimal vatAmount = SupermarketVatCalculator.calculateVatAmount(lineTotal, vatRate);
    BigDecimal lineTotalWithVat = lineTotal.add(vatAmount);

    return new OrderItem(
        lineNumber, row, quantity, unitPrice, lineTotal, vatRate, vatAmount, lineTotalWithVat);
  }

  private BigDecimal extractOptionalBigDecimal(
      int lineNumber, List<String> row, ResolvedMapping resolved, ColumnKey key, String desc) {
    if (!resolved.has(key)) {
      return null;
    }
    ResolvedColumn col = resolved.get(key);
    String rawVal = StrictRowValidator.extractCellValue(lineNumber, row, col.columnIndex(), col.identifier());
    return StrictRowValidator.parseBigDecimal(lineNumber, col.identifier(), rawVal, desc);
  }

  private BigDecimal resolveLineTotal(
      int lineNumber,
      List<String> row,
      ResolvedMapping resolved,
      BigDecimal quantity,
      BigDecimal unitPrice) {
    if (resolved.has(ColumnKey.TOTAL)) {
      ResolvedColumn col = resolved.get(ColumnKey.TOTAL);
      String rawVal = StrictRowValidator.extractCellValue(lineNumber, row, col.columnIndex(), col.identifier());
      BigDecimal parsed = StrictRowValidator.parseBigDecimal(lineNumber, col.identifier(), rawVal, "line total");
      return SupermarketVatCalculator.roundMoney(parsed);
    }

    if (quantity == null || unitPrice == null) {
      throw new CsvProcessingException(
          lineNumber, "TOTAL", "Cannot calculate line total because quantity or unit price is missing");
    }
    return SupermarketVatCalculator.calculateLineTotal(quantity, unitPrice);
  }

  private BigDecimal extractVatRate(int lineNumber, List<String> row, ResolvedMapping resolved) {
    ResolvedColumn vatCol = resolved.get(ColumnKey.VAT_RATE);
    String rawVatVal = StrictRowValidator.extractCellValue(lineNumber, row, vatCol.columnIndex(), vatCol.identifier());
    if (rawVatVal.endsWith("%")) {
      rawVatVal = rawVatVal.substring(0, rawVatVal.length() - 1).trim();
    }
    return StrictRowValidator.parseBigDecimal(lineNumber, vatCol.identifier(), rawVatVal, "VAT rate");
  }

  private String buildOutputCsv(
      List<String> headerRow,
      List<List<String>> enrichedRows,
      OrderSummary summary,
      CsvConfig config) {
    StringBuilder sb = new StringBuilder();
    char delimiter = config.delimiter();

    if (config.hasHeader() && headerRow != null) {
      List<String> outputHeader = new ArrayList<>(headerRow);
      outputHeader.add(VAT_COLUMN_HEADER);
      sb.append(CsvWriter.formatRow(outputHeader, delimiter)).append("\n");
    }

    for (List<String> row : enrichedRows) {
      sb.append(CsvWriter.formatRow(row, delimiter)).append("\n");
    }

    if (config.outputMode() == OutputMode.REPORT_MODE) {
      appendReportFooter(sb, headerRow, enrichedRows, summary, delimiter);
    }

    return sb.toString();
  }

  private void appendReportFooter(
      StringBuilder sb,
      List<String> headerRow,
      List<List<String>> enrichedRows,
      OrderSummary summary,
      char delimiter) {
    int columnCount = enrichedRows.isEmpty()
        ? (headerRow != null ? headerRow.size() + 1 : 2)
        : enrichedRows.get(0).size();
    columnCount = Math.max(columnCount, 2);

    sb.append(buildFooterRow(FOOTER_SUBTOTAL, summary.subtotal().toPlainString(), columnCount, delimiter)).append("\n");
    sb.append(buildFooterRow(FOOTER_TOTAL_VAT, summary.totalVat().toPlainString(), columnCount, delimiter)).append("\n");
    sb.append(buildFooterRow(FOOTER_FINAL_TOTAL, summary.finalTotal().toPlainString(), columnCount, delimiter)).append("\n");
  }

  private String buildFooterRow(String label, String value, int columnCount, char delimiter) {
    List<String> footerCells = new ArrayList<>(columnCount);
    footerCells.add(label);
    for (int i = 1; i < columnCount - 1; i++) {
      footerCells.add("");
    }
    footerCells.add(value);
    return CsvWriter.formatRow(footerCells, delimiter);
  }
}
