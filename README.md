# Outage Work Order API ⚡

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Java Version](https://img.shields.io/badge/Java-17%20LTS-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-success)](#)
[![Automated Tests](https://img.shields.io/badge/Tests-78%20Passed-brightgreen)](#)
[![JaCoCo Coverage](https://img.shields.io/badge/JaCoCo-100%25%20Line%20%26%20Branch-success)](#)
[![RFC 7807](https://img.shields.io/badge/RFC%207807-Problem%20Details-blueviolet)](#)
[![Security Posture](https://img.shields.io/badge/Security-Grade%20A%2B%20(98%2F100)-darkgreen)](#)

Dịch vụ Outage Work Order là một microservice cốt lõi thuộc phân hệ Outage Management System (OMS). API này cung cấp các giao thức RESTful để tạo, quản lý và theo dõi vòng đời của các sự kiện mất điện trên lưới điện.

Dự án áp dụng phương pháp luận **AI-Native SDLC**, phát triển theo mô hình **Spec-Driven Development** dưới sự ràng buộc chặt chẽ của kỹ thuật thiết kế ngữ cảnh (Context Engineering).

---

## Kiến trúc Tổng quan (Architecture Overview)

- **Ngôn ngữ & Nền tảng:** Pure Java 17 records, Spring Boot 3.3.4 (Zero Lombok, Clean Architecture 3 tầng).
- **Cơ sở dữ liệu:** Dual-DB Architecture — H2 In-Memory (Dev/Test) & PostgreSQL (Production), đồng bộ schema bằng **Flyway Migration** (`ddl-auto: validate`).
- **Chuẩn giao tiếp:** RESTful API tuân thủ nghiêm ngặt **RFC 7807 Problem Details** cho 100% các phản hồi lỗi.
- **Bảo mật:** Dual `SecurityFilterChain` (cô lập H2 Console ở `!prod`, HTTP Basic Auth với phân quyền RBAC `@PreAuthorize`).

> [!IMPORTANT]
> **Chính sách Phát triển (Spec-Driven & Zero Drift):** Dự án tuân thủ nguyên tắc "Spec-First": Mọi thay đổi mã nguồn bắt buộc phải đồng bộ 100% với hệ thống tài liệu đặc tả tại [`docs/`](docs/CONTEXT_INDEX.md).

---

## Yêu cầu Hệ thống (Prerequisites)

- [Java Development Kit (JDK) 17 LTS trở lên](https://adoptium.net/)
- [Apache Maven 3.8+](https://maven.apache.org/) (hoặc `./mvnw`)

---

## Hướng dẫn Khởi chạy & Kiểm Thử (Getting Started)

### 1. Kiểm định Toàn diện & Báo cáo JaCoCo (Quality Gate)
```bash
mvn clean verify
```
*Lệnh này chạy toàn bộ 78 automated tests (Unit, Slice, DataJpa, Integration) và thẩm định JaCoCo Quality Gate đạt 100% Line & Branch Coverage.*

### 2. Khởi động Ứng dụng Cục bộ
```bash
mvn spring-boot:run
```

---

## Bảng Điều Khiển Kiểm Thử Trực Quan (Interactive Web Test Console)

Khi ứng dụng chạy, truy cập trình duyệt tại:
👉 **`http://localhost:8080/`**

- **Role Switcher 1-click:** Chuyển đổi giữa `Admin`, `Dispatcher`, `Technician`, và `Anonymous`.
- **Form tạo phiếu:** Kiểm thử validation lỗi HTTP 400 RFC 7807 trực quan.
- **Bảng điều hành thời gian thực:** Xem danh sách phiếu và chuyển trạng thái State Machine (`OPEN` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`).

### Tài khoản Demo Kiểm Thử Cục bộ (`!prod`):
| Username | Password | Roles |
|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_TECHNICIAN` |
| `dispatcher` | `dispatcher123` | `ROLE_DISPATCHER` |
| `technician` | `technician123` | `ROLE_TECHNICIAN` |

> [!TIP]
> Bạn có thể truy cập H2 Web Console tại: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:workorderdb`). Mở công khai ở môi trường non-prod; tự động khóa yêu cầu quyền `ADMIN` trên production.

---

## Tài Liệu Tham Khảo (Documentation)

- **Bản đồ Ngữ cảnh AI:** [`docs/CONTEXT_INDEX.md`](docs/CONTEXT_INDEX.md)
- **Hồ Sơ Bàn Giao Kỹ Thuật:** [`docs/SYSTEM_HANDOVER.md`](docs/SYSTEM_HANDOVER.md)
- **Hồ Sơ Đánh Giá An Ninh Toàn Diện:** [`docs/SECURITY_HANDOVER_REPORT.md`](docs/SECURITY_HANDOVER_REPORT.md)
- **Hợp Đồng REST API:** [`docs/api-spec.md`](docs/api-spec.md)
- **Quy Trình AI-Native SDLC Playbook:** [`docs/prompt/01-sdlc-playbook/00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md`](docs/prompt/01-sdlc-playbook/00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md)
