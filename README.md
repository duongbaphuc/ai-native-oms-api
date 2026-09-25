# Outage Work Order API ⚡

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](#)
[![Release](https://img.shields.io/badge/Release-v1.0.0-blue)](https://github.com/duongbaphuc/ai-native-oms-api/releases/tag/v1.0.0)
[![Java Version](https://img.shields.io/badge/Java-17%20LTS-blue)](#)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-success)](#)
[![Automated Tests](https://img.shields.io/badge/Tests-117%20Passed-brightgreen)](#)
[![JaCoCo Coverage](https://img.shields.io/badge/JaCoCo-100%25%20Line%20%26%20Branch-success)](#)
[![RFC 7807](https://img.shields.io/badge/RFC%207807-Problem%20Details-blueviolet)](#)
[![Security Posture](https://img.shields.io/badge/Security-Review%20Pending-yellow)](#)

Dịch vụ Outage Work Order là một microservice cốt lõi thuộc phân hệ Outage Management System (OMS). API này cung cấp các giao thức RESTful để tạo, quản lý và theo dõi vòng đời của các sự kiện mất điện trên lưới điện.

Dự án áp dụng phương pháp luận **AI-Native SDLC**, phát triển theo mô hình **Spec-Driven Development** dưới sự ràng buộc chặt chẽ của kỹ thuật thiết kế ngữ cảnh (Context Engineering).

> [!NOTE]
> **Phiên bản Phát hành Chính thức (Official Release):** Phiên bản [`v1.0.0`](https://github.com/duongbaphuc/ai-native-oms-api/releases/tag/v1.0.0) là bản phát hành chính thức đầu tiên của Outage Work Order API, đạt chuẩn **100% Zero-Drift**, vượt qua toàn diện 117 automated tests cùng JaCoCo Quality Gate (100% Line & Branch Coverage). Xem chi tiết tại [`CHANGELOG.md`](CHANGELOG.md) và [`docs/11-RELEASE_NOTES_v1.0.0.md`](docs/11-RELEASE_NOTES_v1.0.0.md).

---

## Kiến trúc Tổng quan (Architecture Overview)

- **Ngôn ngữ & Nền tảng:** Pure Java 17 records, Spring Boot 3.3.5 (Zero Lombok, Clean Architecture 3 tầng).
- **Cơ sở dữ liệu:** Dual-DB Architecture — H2 In-Memory (Dev/Test) & PostgreSQL (Production), đồng bộ schema bằng **Flyway Migration** (`ddl-auto: validate`).
- **Chuẩn giao tiếp:** RESTful API tuân thủ nghiêm ngặt **RFC 7807 Problem Details** cho 100% các phản hồi lỗi.
- **Bảo mật & Phòng thủ Chiều sâu:** Dual `SecurityFilterChain` (cô lập H2 Console ở `!prod`, HTTP Basic Auth `!prod`, OAuth2 JWT Resource Server `prod`, Bucket4j Rate Limiting, Distributed Tracing `CorrelationIdFilter`).

> [!IMPORTANT]
> **Chính sách Phát triển (Spec-Driven & Zero Drift):** Dự án tuân thủ nguyên tắc "Spec-First": Mọi thay đổi mã nguồn bắt buộc phải đồng bộ 100% với hệ thống tài liệu đặc tả tại [`docs/`](docs/03-CONTEXT_INDEX.md).

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
*Lệnh này chạy toàn bộ 117 automated tests (Unit, Slice, DataJpa, Integration) và thẩm định JaCoCo Quality Gate đạt 100% Line & Branch Coverage.*

> [!NOTE]
> **Hồ Sơ Kiểm Định & Bằng Chứng Nghiệm Thu Tự Động (Audit Trail Artifacts):**  
> Kết quả thẩm định tự động toàn diện được lưu trữ minh bạch tại:  
> - 📄 Vòng đời nghiệp vụ: [`docs/audit-logs/checklist-work-order-lifecycle-2026-09-25.md`](docs/audit-logs/checklist-work-order-lifecycle-2026-09-25.md)  
> - 🐳 Đóng gói Docker & CI/CD: [`docs/audit-logs/checklist-docker-cicd-2026-09-25.md`](docs/audit-logs/checklist-docker-cicd-2026-09-25.md)  
> - 📑 Đồng bộ bản thảo Zero-Draft-Drift: [`docs/audit-logs/checklist-drafts-synchronization-2026-09-25.md`](docs/audit-logs/checklist-drafts-synchronization-2026-09-25.md)  
> Báo cáo ghi nhận chi tiết 100% tiêu chí đạt chuẩn `[x] PASS`, log execution của 117 tests, JaCoCo Quality Gate 100%, Actuator Probes, và chốt chặn Zero-Drift kiểm toán ngữ cảnh.

### 2. Khởi động Ứng dụng Cục bộ
```bash
# Cách 1: Chạy trực tiếp với Maven (H2 Database in-memory)
mvn spring-boot:run

# Cách 2: Khởi chạy cụm container tích hợp chuẩn Production (API + PostgreSQL)
docker compose up -d
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

## Hệ Thống Tài Liệu Kỹ Thuật (AI-Native SDLC Documentation)

Toàn bộ tài liệu trong thư mục [`docs/`](docs/03-CONTEXT_INDEX.md) được sắp xếp chặt chẽ theo thứ tự các giai đoạn của quy trình AI-Native SDLC:

| Giai Đoạn SDLC | Tài Liệu Kỹ Thuật | Vai Trò / Trọng Tâm |
|---|---|---|
| **Pha 00: Governance & Rules** | [`docs/00-coding-rules.md`](docs/00-coding-rules.md)<br>[`docs/00-internal-coding-standards.md`](docs/00-internal-coding-standards.md)<br>[`docs/00-api-rules.md`](docs/00-api-rules.md)<br>[`docs/00-security-rules.md`](docs/00-security-rules.md) | Quy chuẩn viết mã Oracle Core, tiêu chuẩn thiết kế API RESTful, an ninh OWASP API và Instant UTC |
| **Pha 01: Business & Domain** | [`docs/01-br-analysis-wo.md`](docs/01-br-analysis-wo.md)<br>[`docs/01-domain-model.md`](docs/01-domain-model.md) | Phân tích bài toán mất điện lưới, mô hình Aggregate Root, Value Objects và State Machine |
| **Pha 02: Architecture & Specs** | [`docs/02-api-spec.md`](docs/02-api-spec.md)<br>[`docs/02-database-migration-spec.md`](docs/02-database-migration-spec.md)<br>[`docs/02-security-auth-spec.md`](docs/02-security-auth-spec.md)<br>[`docs/02-observability-and-logging.md`](docs/02-observability-and-logging.md)<br>[`docs/02-ADR-001-use-h2-database.md`](docs/02-ADR-001-use-h2-database.md) | Hợp đồng REST API RFC 7807, Flyway DDL migration, Dual SecurityFilterChain, ECS Logging & ADR |
| **Pha 03: AI Context Index** | [`docs/03-CONTEXT_INDEX.md`](docs/03-CONTEXT_INDEX.md) | Bản đồ điều hướng ngữ cảnh AI và công thức nạp Modular Context |
| **Pha 04: Implementation Drafts** | [`docs/drafts/`](docs/drafts/) | 12 bản thảo kỹ thuật chi tiết (Blueprints) bao quát 39 tệp mã nguồn với tỷ lệ 100% Zero-Draft-Drift |
| **Pha 08: System Handover** | [`docs/08-SYSTEM_HANDOVER.md`](docs/08-SYSTEM_HANDOVER.md)<br>[`docs/08-ORACLE_JAVA_DOCUMENTATION.md`](docs/08-ORACLE_JAVA_DOCUMENTATION.md) | Hồ sơ bàn giao kỹ thuật toàn diện (117 tests, Runbook) và Cẩm nang kiến trúc Java Enterprise |
| **Pha 09: Security Audit** | [`docs/09-SECURITY_HANDOVER_REPORT.md`](docs/09-SECURITY_HANDOVER_REPORT.md) | Báo cáo thẩm định an ninh bàn giao, kiểm toán OWASP API Top 10 & CWE |
| **Pha 10: DevOps & CI/CD** | [`docs/10-devops-pipeline-spec.md`](docs/10-devops-pipeline-spec.md) | Đặc tả Multi-stage Dockerfile non-root, Docker Compose & GitHub Actions CI |
| **Pha 11: Release Management** | [`CHANGELOG.md`](CHANGELOG.md)<br>[`docs/11-RELEASE_NOTES_v1.0.0.md`](docs/11-RELEASE_NOTES_v1.0.0.md) | Hồ sơ phát hành chính thức v1.0.0 và nhật ký thay đổi phiên bản |
| **Pha 15: Automated Audit** | [`docs/audit-logs/checklist-work-order-lifecycle-2026-09-25.md`](docs/audit-logs/checklist-work-order-lifecycle-2026-09-25.md) | Hồ sơ checklist nghiệm thu tự động, 100% tiêu chí kỹ thuật có log bằng chứng thực thi |
| **Playbook Trọn Gói** | [`docs/prompt/01-sdlc-playbook/00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md`](docs/prompt/01-sdlc-playbook/00-MASTER-AI-NATIVE-SDLC-PLAYBOOK.md) | Chuỗi 17 prompt chuẩn mực từ Pha 00 đến Pha 15 (kèm Pha 04B) phục vụ AI-Native SDLC |
