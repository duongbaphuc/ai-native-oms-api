package com.gpc.order.processor.example;

import com.gpc.order.processor.api.CsvOrderProcessor;
import com.gpc.order.processor.api.config.ColumnKey;
import com.gpc.order.processor.api.config.MetadataConfig;
import com.gpc.order.processor.api.config.OutputMode;
import com.gpc.order.processor.api.exception.CsvProcessingException;
import com.gpc.order.processor.api.model.CsvProcessingResult;
import com.gpc.order.processor.api.model.Order;
import com.gpc.order.processor.api.model.OrderItem;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ví dụ minh họa thực tế (Example Usage) về cách một dự án hoặc module khác
 * import file thư viện (.jar) và gọi các Public API của CsvOrderProcessor.
 */
public class ExampleUsage {

  public static void main(String[] args) throws Exception {
    System.out.println("===============================================================");
    System.out.println("  MINH HỌA SỬ DỤNG THƯ VIỆN CSV ORDER PROCESSOR (PUBLIC API)   ");
    System.out.println("===============================================================\n");

    // Khởi tạo đối tượng Facade duy nhất thông qua Factory Method
    CsvOrderProcessor processor = CsvOrderProcessor.create();

    // -------------------------------------------------------------------------
    // VÍ DỤ 1: Đọc qua InputStream với MetadataConfig ở chế độ DATA_MODE
    //          Mục tiêu: Nhận kết quả có cấu trúc đối tượng (Structured Data)
    // -------------------------------------------------------------------------
    System.out.println("--- VÍ DỤ 1: InputStream + DATA_MODE (Nhận Cấu Trúc Dữ Liệu) ---");
    String csvInputData = """
        Mã hàng,Tên hàng hóa,Số lượng,Đơn giá,% VAT
        SP-01,Gạo ST25 5kg,2,180000.00,5
        SP-02,Dầu ăn Simply 1L,3,65000.00,10
        SP-03,Nước mắm Nam Ngư,1,42000.00,8
        """;

    InputStream inputStream = new ByteArrayInputStream(csvInputData.getBytes(StandardCharsets.UTF_8));

    MetadataConfig dataModeConfig = MetadataConfig.builder()
        .hasHeader(true)
        .delimiter(',')
        .mapColumn("Số lượng", ColumnKey.QUANTITY)
        .mapColumn("Đơn giá", ColumnKey.UNIT_PRICE)
        .mapColumn("% VAT", ColumnKey.VAT_RATE)
        .outputMode(OutputMode.DATA_MODE)
        .build();

    CsvProcessingResult dataResult = processor.process(inputStream, dataModeConfig);

    // 1.1. Lấy dữ liệu tổng hợp (OrderSummary)
    System.out.println("Tổng tiền hàng trước thuế (Subtotal): " + dataResult.summary().subtotal());
    System.out.println("Tổng tiền thuế VAT (Total VAT):       " + dataResult.summary().totalVat());
    System.out.println("Tổng thanh toán cuối cùng:             " + dataResult.summary().finalTotal());

    // 1.2. Duyệt qua từng dòng mặt hàng trong bộ nhớ (OrderItem)
    System.out.println("\nChi tiết các dòng mặt hàng đã tính toán VAT:");
    for (OrderItem item : dataResult.lineItems()) {
      System.out.printf("  - Dòng %d: Thành tiền = %s | VAT (%s%%) = %s | Tổng sau VAT = %s%n",
          item.lineNumber(),
          item.lineTotal(),
          item.vatRate(),
          item.vatAmount(),
          item.lineTotalWithVat());
    }

    // 1.3. Ánh xạ trực tiếp sang Aggregate Root Order
    Order order = dataResult.toOrder("ORDER-HCM-2026-001");
    System.out.println("\nĐã đóng gói thành Aggregate Root Order: ID = " + order.orderId());
    System.out.println("Số lượng items trong Order: " + order.items().size());

    // -------------------------------------------------------------------------
    // VÍ DỤ 2: Đọc qua Path tệp tin với MetadataConfig ở chế độ REPORT_MODE
    //          Mục tiêu: Lấy mảng byte (byte[]) của file CSV đã thêm dòng tổng
    // -------------------------------------------------------------------------
    System.out.println("\n--- VÍ DỤ 2: Path File + REPORT_MODE (Lấy Mảng Byte CSV) ---");
    Path tempCsvFile = Files.createTempFile("order_sample_", ".csv");
    String csvReportData = """
        Mã SP;Tên sản phẩm;Số lượng;Đơn giá;Thuế suất
        IP15;iPhone 15 Pro;1;27000000.00;10
        AP02;AirPods Pro 2;2;5500000.00;8
        """;
    Files.writeString(tempCsvFile, csvReportData, StandardCharsets.UTF_8);

    MetadataConfig reportModeConfig = MetadataConfig.builder()
        .hasHeader(true)
        .delimiter(';')
        .mapColumn("Số lượng", ColumnKey.QUANTITY)
        .mapColumn("Đơn giá", ColumnKey.UNIT_PRICE)
        .mapColumn("Thuế suất", ColumnKey.VAT_RATE)
        .outputMode(OutputMode.REPORT_MODE)
        .build();

    CsvProcessingResult reportResult = processor.process(tempCsvFile, reportModeConfig);

    // Lấy trực tiếp mảng byte (byte[]) để trả về HTTP response hoặc lưu xuống đĩa
    byte[] csvBytes = reportResult.getCsvBytes();
    System.out.println("Độ dài mảng byte kết quả: " + csvBytes.length + " bytes");
    System.out.println("Nội dung CSV báo cáo (đã thêm cột Tiền VAT và 3 dòng footer tổng cộng):");
    System.out.println(new String(csvBytes, StandardCharsets.UTF_8));

    // Dọn dẹp tệp tạm
    Files.deleteIfExists(tempCsvFile);

    // -------------------------------------------------------------------------
    // VÍ DỤ 3: Xử lý ngoại lệ dữ liệu lỗi (CsvProcessingException)
    // -------------------------------------------------------------------------
    System.out.println("--- VÍ DỤ 3: Bắt và Xử Lý Ngoại Lệ CsvProcessingException ---");
    String invalidCsvData = """
        Mã SP,Số lượng,Đơn giá,% VAT
        SP-01,10,50000.00,10
        SP-02,KhôngPhảiSố,75000.00,10
        """;

    try {
      InputStream invalidStream = new ByteArrayInputStream(invalidCsvData.getBytes(StandardCharsets.UTF_8));
      processor.process(invalidStream, dataModeConfig);
    } catch (CsvProcessingException ex) {
      System.err.println(">> Đã bắt được ngoại lệ CsvProcessingException:");
      System.err.println("   + Dòng lỗi trong file: " + ex.getLineNumber());
      System.err.println("   + Cột phát sinh lỗi:   " + ex.getColumnIdentifier());
      System.err.println("   + Chi tiết thông báo:  " + ex.getMessage());
    }

    System.out.println("\n===============================================================");
    System.out.println("               CHƯƠNG TRÌNH MINH HỌA KẾT THÚC                  ");
    System.out.println("===============================================================");
  }
}
