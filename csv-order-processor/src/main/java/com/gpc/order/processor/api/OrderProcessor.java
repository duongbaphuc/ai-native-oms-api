package com.gpc.order.processor.api;

import com.gpc.order.processor.api.config.CsvConfig;
import com.gpc.order.processor.api.model.CsvProcessingResult;
import com.gpc.order.processor.api.model.Order;

import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;

/**
 * Giao diện cơ sở tổng quát đại diện cho một bộ xử lý đơn hàng.
 * Cho phép mở rộng hỗ trợ nhiều định dạng đầu vào khác nhau (CSV, Excel, JSON...) trong tương lai.
 */
public interface OrderProcessor {

  /**
   * Xử lý dữ liệu đơn hàng từ chuỗi văn bản CSV.
   *
   * @param content chuỗi văn bản CSV đầu vào
   * @param config cấu hình ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu và đối tượng tổng hợp
   */
  CsvProcessingResult process(String content, CsvConfig config);

  /**
   * Xử lý dữ liệu đơn hàng từ một Reader.
   *
   * @param reader đối tượng đọc luồng ký tự CSV
   * @param config cấu hình ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu và đối tượng tổng hợp
   */
  CsvProcessingResult process(Reader reader, CsvConfig config);

  /**
   * Xử lý dữ liệu đơn hàng từ một InputStream dạng byte.
   *
   * @param inputStream luồng dữ liệu byte CSV (sẽ đọc theo chuẩn UTF-8)
   * @param config cấu hình ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu và đối tượng tổng hợp
   */
  CsvProcessingResult process(InputStream inputStream, CsvConfig config);

  /**
   * Xử lý dữ liệu đơn hàng từ một đường dẫn tệp tin trên hệ thống tập tin.
   *
   * @param filePath đường dẫn tệp tin CSV
   * @param config cấu hình ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu và đối tượng tổng hợp
   */
  CsvProcessingResult process(Path filePath, CsvConfig config);

  /**
   * Xử lý dữ liệu đơn hàng từ một InputStream dạng byte với MetadataConfig.
   *
   * @param inputStream luồng dữ liệu byte CSV (sẽ đọc theo chuẩn UTF-8)
   * @param config cấu hình metadata ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu, byte array và đối tượng tổng hợp
   */
  default CsvProcessingResult process(
      InputStream inputStream, com.gpc.order.processor.api.config.MetadataConfig config) {
    java.util.Objects.requireNonNull(config, "config must not be null");
    return process(inputStream, config.toCsvConfig());
  }

  /**
   * Xử lý dữ liệu đơn hàng từ một đường dẫn tệp tin trên hệ thống tập tin với MetadataConfig.
   *
   * @param filePath đường dẫn tệp tin CSV
   * @param config cấu hình metadata ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu, byte array và đối tượng tổng hợp
   */
  default CsvProcessingResult process(
      Path filePath, com.gpc.order.processor.api.config.MetadataConfig config) {
    java.util.Objects.requireNonNull(config, "config must not be null");
    return process(filePath, config.toCsvConfig());
  }

  /**
   * Xử lý dữ liệu đơn hàng từ chuỗi văn bản CSV với MetadataConfig.
   *
   * @param content chuỗi văn bản CSV đầu vào
   * @param config cấu hình metadata ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu, byte array và đối tượng tổng hợp
   */
  default CsvProcessingResult process(
      String content, com.gpc.order.processor.api.config.MetadataConfig config) {
    java.util.Objects.requireNonNull(config, "config must not be null");
    return process(content, config.toCsvConfig());
  }

  /**
   * Xử lý dữ liệu đơn hàng từ một Reader với MetadataConfig.
   *
   * @param reader đối tượng đọc luồng ký tự CSV
   * @param config cấu hình metadata ánh xạ cột, dấu phân cách và chế độ xuất
   * @return kết quả xử lý gồm chuỗi CSV làm giàu, byte array và đối tượng tổng hợp
   */
  default CsvProcessingResult process(
      Reader reader, com.gpc.order.processor.api.config.MetadataConfig config) {
    java.util.Objects.requireNonNull(config, "config must not be null");
    return process(reader, config.toCsvConfig());
  }

  /**
   * Xử lý dữ liệu đơn hàng từ chuỗi văn bản CSV và ánh xạ trực tiếp sang Aggregate Root Order.
   *
   * @param orderId mã định danh đơn hàng
   * @param content chuỗi văn bản CSV đầu vào
   * @param config  cấu hình ánh xạ cột
   * @return đối tượng Order hoàn chỉnh
   */
  default Order processToOrder(String orderId, String content, CsvConfig config) {
    return process(content, config).toOrder(orderId);
  }

  /**
   * Xử lý dữ liệu đơn hàng từ tệp tin CSV và ánh xạ trực tiếp sang Aggregate Root Order.
   *
   * @param orderId  mã định danh đơn hàng
   * @param filePath đường dẫn tệp tin CSV
   * @param config   cấu hình ánh xạ cột
   * @return đối tượng Order hoàn chỉnh
   */
  default Order processToOrder(String orderId, Path filePath, CsvConfig config) {
    return process(filePath, config).toOrder(orderId);
  }

  /**
   * Factory method tiện ích lấy bộ xử lý cho định dạng CSV.
   *
   * @return một thể hiện mặc định của CsvOrderProcessor
   */
  static CsvOrderProcessor forCsv() {
    return CsvOrderProcessor.create();
  }
}
