// Nguồn gốc AI: sinh từ docs/00-coding-rules.md, docs/01-domain-model.md
package com.gpc.oms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lớp khởi động chính của phân hệ Outage Management System (OMS) REST API.
 *
 * <p>Cấu hình và khởi chạy ứng dụng Spring Boot 3.3 quản lý vòng đời phiếu công tác sự cố lưới điện.</p>
 *
 * @author Đội ngũ Kiến trúc GPC OMS
 * @version 1.0.0
 * @since 1.0.0
 */
@SpringBootApplication
public class OmsApiApplication {

    /**
     * Điểm vào chính của tiến trình thực thi JVM.
     *
     * @param args Các đối số dòng lệnh truyền vào khi khởi chạy
     */
    public static void main(String[] args) {
        SpringApplication.run(OmsApiApplication.class, args);
    }
}
