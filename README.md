# Outage Work Order API ⚡

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-success)](#)

Dịch vụ Outage Work Order là một microservice cốt lõi thuộc phân hệ Outage Management System (OMS)[cite: 6]. API này cung cấp các giao thức RESTful để tạo, quản lý và theo dõi vòng đời của các sự kiện mất điện trên lưới điện.

Dự án áp dụng phương pháp luận **AI-Native SDLC**, sử dụng GitHub Copilot dưới sự ràng buộc chặt chẽ của kỹ thuật thiết kế ngữ cảnh (Context Engineering)[cite: 1, 5].

## Kiến trúc Tổng quan (Architecture Overview)

- **Framework:** Java 17, Spring Boot 3.3[cite: 8].
- **Data Store:** H2 Database (In-memory) - Tối ưu cho môi trường Lab/Demo.
- **Thiết kế API:** RESTful tuân thủ chuẩn RFC 7807 (Problem Details)[cite: 5, 8].

> [!IMPORTANT]
> **Chính sách Phát triển (Spec-Driven):** Dự án tuân thủ nguyên tắc không sinh mã nguồn tính năng nếu chưa phê duyệt tài liệu đặc tả (Domain Model & API Spec)[cite: 4].

## Yêu cầu Hệ thống (Prerequisites)

- [Java Development Kit (JDK) 17+](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/)

## Hướng dẫn Khởi chạy (Getting Started)

**Bước 1: Tải dependencies và biên dịch**
```bash
./mvnw clean install -DskipTests
```

**Bước 2: Khởi động dịch vụ**
```bash
./mvnw spring-boot:run
```

> [!TIP]
> Ứng dụng khởi chạy tại http://localhost:8080. Bạn có thể truy cập http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:workorderdb) để trực tiếp xem các thay đổi dữ liệu trong bộ nhớ.
