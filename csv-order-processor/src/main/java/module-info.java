module com.gpc.order.processor {
  // Chỉ xuất bản các Package thuộc Public API
  exports com.gpc.order.processor.api;
  exports com.gpc.order.processor.api.config;
  exports com.gpc.order.processor.api.model;
  exports com.gpc.order.processor.api.exception;

  // Package 'com.gpc.order.processor.internal.*' TUYỆT ĐỐI KHÔNG EXPORTS
  // -> Bất kỳ dự án nào import JAR đều KHÔNG THỂ truy cập hay gọi các class internal!
}
