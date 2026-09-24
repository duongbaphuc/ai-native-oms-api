package com.gpc.order.processor.api.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata cấu hình truyền vào thư viện để ánh xạ động dữ liệu đơn hàng CSV.
 *
 * @param hasHeader Xác định tệp CSV có dòng tiêu đề hay không.
 * @param delimiter Ký tự phân cách đang sử dụng (thông thường là ',' hoặc ';').
 * @param columnMapping Ánh xạ vị trí các cột cần thiết (Số lượng, Đơn giá, Số tổng, % VAT) thông qua
 *        tên cột (String) nếu có header, hoặc chỉ số cột 0-based (Integer) nếu không có header.
 * @param outputMode Chế độ đầu ra (REPORT_MODE hoặc DATA_MODE).
 */
public record CsvConfig(
    boolean hasHeader,
    char delimiter,
    Map<Object, ColumnKey> columnMapping,
    OutputMode outputMode) {

  public CsvConfig {
    Objects.requireNonNull(columnMapping, "columnMapping must not be null");
    Objects.requireNonNull(outputMode, "outputMode must not be null");

    if (delimiter == '\0' || delimiter == '\r' || delimiter == '\n' || delimiter == '"') {
      throw new IllegalArgumentException("Invalid CSV delimiter character: '" + delimiter + "'");
    }

    if (columnMapping.isEmpty()) {
      throw new IllegalArgumentException("columnMapping must contain at least required columns");
    }

    columnMapping = Collections.unmodifiableMap(new HashMap<>(columnMapping));
  }

  /**
   * Tạo đối tượng Builder để thiết lập cấu hình CsvConfig từng bước linh hoạt.
   *
   * @return đối tượng Builder mới
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Bộ dựng (Builder) hỗ trợ xây dựng đối tượng CsvConfig bất biến theo mẫu Fluent API.
   */
  public static class Builder {
    private boolean hasHeader = true;
    private char delimiter = ',';
    private final Map<Object, ColumnKey> columnMapping = new HashMap<>();
    private OutputMode outputMode = OutputMode.DATA_MODE;

    /**
     * Chỉ định file CSV có dòng tiêu đề hay không. Mặc định là true.
     *
     * @param hasHeader true nếu có header, false nếu không có
     * @return đối tượng Builder hiện tại
     */
    public Builder hasHeader(boolean hasHeader) {
      this.hasHeader = hasHeader;
      return this;
    }

    /**
     * Chỉ định ký tự phân cách các cột trong CSV. Mặc định là dấu phẩy ','.
     *
     * @param delimiter ký tự phân tách (vd: ',' hoặc ';')
     * @return đối tượng Builder hiện tại
     */
    public Builder delimiter(char delimiter) {
      this.delimiter = delimiter;
      return this;
    }

    /**
     * Ánh xạ một cột cụ thể vào khóa nghiệp vụ chuẩn.
     *
     * @param identifier tên cột (String) nếu có header, hoặc chỉ số cột 0-based (Integer) nếu không
     *        có header
     * @param columnKey loại cột nghiệp vụ tương ứng (QUANTITY, UNIT_PRICE, TOTAL, VAT_RATE)
     * @return đối tượng Builder hiện tại
     */
    public Builder mapColumn(Object identifier, ColumnKey columnKey) {
      this.columnMapping.put(identifier, columnKey);
      return this;
    }

    /**
     * Thiết lập toàn bộ bảng ánh xạ các cột.
     *
     * @param mapping bản đồ ánh xạ từ định danh cột sang khóa nghiệp vụ
     * @return đối tượng Builder hiện tại
     */
    public Builder columnMapping(Map<?, ColumnKey> mapping) {
      this.columnMapping.clear();
      if (mapping != null) {
        this.columnMapping.putAll(mapping);
      }
      return this;
    }

    /**
     * Chỉ định chế độ xuất kết quả (REPORT_MODE có footer tổng cộng, hoặc DATA_MODE thuần bảng mặt
     * hàng). Mặc định là DATA_MODE.
     *
     * @param outputMode chế độ xuất kết quả
     * @return đối tượng Builder hiện tại
     */
    public Builder outputMode(OutputMode outputMode) {
      this.outputMode = outputMode;
      return this;
    }

    /**
     * Kiểm tra tính hợp lệ và hoàn tất khởi tạo đối tượng CsvConfig bất biến.
     *
     * @return đối tượng CsvConfig đã hoàn tất cấu hình
     */
    public CsvConfig build() {
      return new CsvConfig(hasHeader, delimiter, columnMapping, outputMode);
    }
  }
}
