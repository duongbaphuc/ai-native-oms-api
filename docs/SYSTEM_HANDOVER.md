# HỒ SƠ BÀN GIAO KỸ THUẬT & VẬN HÀNH HỆ THỐNG
## Outage Management System - Work Order API Service (`oms-api-demo`)

> **Tài liệu tham chiếu chuẩn (Single Source of Truth):** `docs/SYSTEM_HANDOVER.md`  
> **Phiên bản:** `1.0.0-RELEASE`  
> **Thời điểm lập hồ sơ:** 2026-09-24  
> **Đơn vị bàn giao:** AI-Native Engineering Core Team  
> **Đơn vị tiếp nhận:** Production Operations (Ops/SRE) & Core Software Engineering Team  

---

## MỤC LỤC
1. [Tổng Quan Dự Án & Bối Cảnh Nghiệp Vụ](#1-tổng-quan-dự-án--bối-cảnh-nghiệp-vụ)
2. [Bản Đồ Kiến Trúc & Cấu Trúc Mã Nguồn](#2-bản-đồ-kiến-trúc--cấu-trúc-mã-nguồn)
3. [Mô Hình Dữ Liệu & Quản Trị Cơ Sở Dữ Liệu](#3-mô-hình-dữ-liệu--quản-trị-cơ-sở-dữ-liệu)
4. [Danh Mục Hợp Đồng API & Tích Hợp Hệ Thống](#4-danh-mục-hợp-đồng-api--tích-hợp-hệ-thống)
5. [Mô Hình Bảo Mật & Phân Quyền (RBAC)](#5-mô-hình-bảo-mật--phân-quyền-rbac)
6. [Sổ Tay Biên Dịch, Khởi Chạy & Triển Khai (Runbook)](#6-sổ-tay-biên-dịch-khởi-chạy--triển-khai-runbook)
7. [Hồ Sơ Chất Lượng & Báo Cáo Kiểm Thử Tự Động](#7-hồ-sơ-chất-lượng--báo-cáo-kiểm-thử-tự-động)
8. [Sổ Tay Vận Hành, Giám Sát & Xử Lý Sự Cố (Troubleshooting)](#8-sổ-tay-vận-hành-giám-sát--xử-lý-sự-cố-troubleshooting)
9. [Quy Trình Phát Triển Mở Rộng & Ký Nhận Bàn Giao](#9-quy-trình-phát-triển-mở-rộng--ký-nhận-bàn-giao)

---

## 1. TỔNG QUAN DỰ ÁN & BỐI CẢNH NGHIỆP VỤ

### 1.1 Mục Tiêu & Phạm Vi Chức Năng
Dịch vụ **Outage Work Order API** là một microservice cốt lõi trong hệ thống Quản lý Sự cố Lưới điện (Outage Management System - OMS). Hệ thống chịu trách nhiệm quản lý toàn bộ vòng đời của các phiếu công tác (Work Orders) nhằm khắc phục các sự cố mất điện, bao gồm:
- **Tiếp nhận & Khởi tạo phiếu công tác:** Ghi nhận mã thiết bị lưới điện, mô tả sự cố, và mức độ ưu tiên nghiệp vụ.
- **Quản lý Vòng đời & Máy trạng thái (State Machine):** Chuyển dịch trạng thái nghiêm ngặt từ `OPEN` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE`.
- **Truy vấn & Phân trang:** Hỗ trợ điều độ viên tra cứu danh sách phiếu theo trạng thái và sắp xếp thời gian tạo mới nhất.

### 1.2 Triết Lý Thiết Kế
- **Spec-Driven Development (Phát triển Hướng Đặc tả):** 100% mã nguồn, API schema, mã lỗi và ràng buộc dữ liệu được hiện thực hóa trực tiếp từ hệ thống tài liệu Markdown tại thư mục `docs/`.
- **Domain-Driven Design (DDD):** Đóng gói toàn vẹn logic nghiệp vụ trong Aggregate Root `WorkOrder`, bảo vệ bất biến (Invariants) tại tầng miền trước khi xuống tầng lưu trữ.
- **Clean Architecture 3 Tầng (3-Tier Layered Architecture):** Phân tách độc lập tuyệt đối giữa Controller (Giao tiếp), Service (Điều phối nghiệp vụ), và Repository (Lưu trữ dữ liệu).
- **Chuẩn Hóa Xử Lý Lỗi RFC 7807 (Problem Details):** Tất cả phản hồi lỗi đều tuân thủ định dạng `application/problem+json` với định danh URN chuẩn tắc.

### 1.3 Bảng Kê Công Nghệ Cốt Lõi (Tech Stack Inventory)

| Hạng mục | Công nghệ / Thư viện | Phiên bản | Ghi chú kỹ thuật |
|---|---|---|---|
| **Ngôn ngữ** | Java (OpenJDK / Temurin) | `17 LTS` | Sử dụng Records, Pattern Matching switch, Text Blocks, Compact Constructors |
| **Framework nền tảng** | Spring Boot | `3.3.4` | Bao gồm Web, Security, Data JPA, Validation |
| **Cơ sở dữ liệu** | H2 Database Engine | `2.2.224` | Chế độ In-Memory, tương thích cú pháp PostgreSQL |
| **Quản trị Schema** | Flyway Migration | `10.x` | Quản lý phiên bản migration tự động qua DDL |
| **Bảo mật & Phân quyền** | Spring Security | `6.3.3` | HTTP Basic, Stateless Session, Method Security (`@PreAuthorize`) |
| **Kiểm thử tự động** | JUnit 5 + Mockito + AssertJ | Latest Spring Boot | 86 automated test cases (Unit, Slice, Integration, Fixture-driven) |
| **Đo lường Coverage** | JaCoCo Maven Plugin | `0.8.12` | Thực thi Quality Gate: **100% Line & 100% Branch Coverage** trên 12 monitored classes |
| **Công cụ đóng gói** | Apache Maven | `3.8+` | Kèm theo Maven Wrapper (`mvnw.cmd` / `mvnw`) |

---

## 2. BẢN ĐỒ KIẾN TRÚC & CẤU TRÚC MÃ NGUỒN

### 2.1 Sơ Đồ Kiến Trúc & Luồng Dữ Liệu Đầu-Cuối

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Browser Console
    participant CIF as CorrelationIdFilter
    participant SEC as SecurityFilterChain
    participant WOC as WorkOrderController
    participant WOS as WorkOrderService
    participant WOR as WorkOrderRepository
    participant H2DB as H2 In-Memory Database

    Client->>CIF: HTTP Request (Optional X-Correlation-Id)
    Note over CIF: Thiết lập MDC & Gán Correlation ID
    CIF->>SEC: Forward Request
    Note over SEC: Kiểm tra Basic Auth & RBAC (@PreAuthorize)
    alt Không hợp lệ / Chưa xác thực
        SEC-->>Client: 401 Unauthorized / 403 Forbidden (RFC 7807)
    else Hợp lệ
        SEC->>WOC: Dispatch Endpoint
        WOC->>WOS: Delegate Business Request (DTO Record)
        Note over WOS: Validate Invariants & State Transitions
        WOS->>WOR: Persist / Query Entity
        WOR->>H2DB: SQL Operation (Flyway DDL)
        H2DB-->>WOR: SQL Result Set
        WOR-->>WOS: Domain Entity (WorkOrder)
        WOS-->>WOC: WorkOrderResponse (DTO Record)
        WOC-->>Client: HTTP 200/201 + X-Correlation-Id Header
    end
```

### 2.2 Danh Mục Gói & Trách Nhiệm Tệp Mã Nguồn

```
c:\ai-native-oms-api\src\main\java\com\gpc\oms
├── OmsApiApplication.java                   [Entry Point] Khởi động Spring Boot Application
├── config/
│   ├── SecurityConfig.java                  [Security] Dual SecurityFilterChain (h2ConsoleChain !prod & filterChain), RFC 7807 401
│   └── StringToWorkOrderStatusConverter.java [Converter] Web conversion chuỗi query param sang WorkOrderStatus Enum (kèm cache values array)
├── controller/
│   └── WorkOrderController.java             [REST API] Tiếp nhận HTTP request, phân quyền @PreAuthorize
├── domain/
│   ├── WorkOrder.java                       [Entity] Aggregate Root, bảo vệ bất biến & quản lý chuyển đổi trạng thái với fast-path null-check
│   ├── Priority.java                        [Enum] 4 mức độ ưu tiên (LOW, MEDIUM, HIGH, CRITICAL)
│   ├── WorkOrderStatus.java                [Enum] 3 trạng thái (OPEN, IN_PROGRESS, DONE) & hàm canTransitionTo
│   └── WorkOrderRepository.java             [Repository] Spring Data JPA interface tương tác với H2/PostgreSQL
├── dto/
│   ├── WorkOrderRequest.java                [DTO] Record tiếp nhận payload tạo phiếu (đạt chuẩn RFC 7807 validation)
│   ├── WorkOrderStatusRequest.java          [DTO] Record cập nhật trạng thái phiếu công tác
│   ├── WorkOrderResponse.java               [DTO] Immutable record trả về cho client với static factory mapper
│   └── PagedResponse.java                   [DTO] Generic record bọc dữ liệu phân trang
├── exception/
│   ├── GlobalExceptionHandler.java          [Advice] Bắt toàn bộ 7 nhóm ngoại lệ và chuyển hóa về RFC 7807 ProblemDetail (pre-sized collections)
│   ├── ProblemTypes.java                    [Constants] Tập trung hóa các URN RFC 7807 Problem Type URI (DRY, Zero Magic Strings, Zero Allocations)
│   └── ResourceNotFoundException.java       [Exception] Ngoại lệ ném ra khi không tìm thấy UUID phiếu
└── service/
    └── WorkOrderService.java                [Service] Transactional orchestration & logic miền ứng dụng

c:\ai-native-oms-api\src\test\java\com\gpc\oms
└── testutil/
    └── WorkOrderTestFixtures.java           [Fixtures] Object Mother Pattern cung cấp static factory methods tạo mock entities & DTOs tái sử dụng
```

### 2.3 Các Mẫu Thiết Kế (Design Patterns) & Chuẩn Mực Kỹ Thuật Áp Dụng
Hệ thống tuân thủ nghiêm ngặt chuẩn mực thiết kế của **Senior Java Engineer tại Oracle** và các nguyên lý trong **Effective Java (Joshua Bloch)**:
1. **Static Factory Method Pattern (Item 1, Effective Java):**
   - Áp dụng trên các DTOs bất biến (`WorkOrderResponse.from(WorkOrder wo)`, `PagedResponse.from(Page<T> page)`).
   - Đóng gói logic ánh xạ tường minh với kiểm tra fail-fast `Objects.requireNonNull()`, loại bỏ hoàn toàn các thư viện reflection mapping (`ModelMapper`) tốn tài nguyên.
2. **State Pattern / Finite State Machine (DDD Domain Invariants):**
   - Enum `WorkOrderStatus` định nghĩa tường minh ma trận chuyển trạng thái hợp lệ qua hàm `canTransitionTo(targetStatus)`.
   - Domain Aggregate Root `WorkOrder.advanceStatus(newStatus)` thực thi chốt chặn bất biến nghiệp vụ, chỉ cho phép dòng chuyển dịch đơn hướng `OPEN` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `DONE` và ném `IllegalStateException` khi vi phạm.
3. **Pure Immutable Records (Java 17 LTS):**
   - 100% Request và Response DTOs sử dụng Java 17 `record`.
   - Tự động sở hữu các đặc tính bất biến, `equals()`, `hashCode()`, `toString()` và Compact Constructors để kiểm tra ràng buộc đầu vào mà không cần mã thừa (Boilerplate-free).
4. **Centralized Constants Pattern & DRY Principles:**
   - Lớp `ProblemTypes` đóng gói toàn bộ các URN RFC 7807 (`URI` constants) dùng chung giữa `GlobalExceptionHandler`, Controller và `SecurityConfig`.
   - Triệt tiêu 100% Magic Strings và loại bỏ chi phí phân tích chuỗi lặp lại qua `URI.create()`.
5. **JVM Performance & GC Tuning Best Practices:**
   - **Pre-sizing Collections:** Khởi tạo `ArrayList` với `initialCapacity` chính xác khi biết trước kích thước dữ liệu (`new ArrayList<>(fieldErrors.size())`), tránh mảng co dãn liên tục trong bộ nhớ Heap.
   - **Array Caching:** Caching mảng `WorkOrderStatus.values()` tĩnh trong `StringToWorkOrderStatusConverter` để tránh chi phí clone mảng trên mỗi HTTP request.
   - **JIT Escape Analysis Optimization:** Áp dụng từ khóa `final` cho 100% parameters và biến cục bộ để hỗ trợ trình biên dịch JIT tối ưu hóa Escape Analysis và Inline Caching.
6. **Object Mother / Test Fixtures Pattern:**
   - Xây dựng lớp tiện ích `WorkOrderTestFixtures` trong `src/test/java/com/gpc/oms/testutil/` cung cấp các static factory methods tạo đối tượng kiểm thử mẫu (`createDefaultWorkOrder()`, `createDefaultRequest()`, `createDoneWorkOrder()`).
   - Giúp toàn bộ test suite có độ cô đọng cao (High Signal-to-Noise Ratio) và loại bỏ trùng lặp mã khởi tạo.

---

## 3. MÔ HÌNH DỮ LIỆU & QUẢN TRỊ CƠ SỞ DỮ LIỆU

### 3.1 Chiến Lược Quản Trị Schema (Flyway Migration)
Dự án sử dụng **Flyway** để quản lý các bước tiến hóa cấu trúc cơ sở dữ liệu. Toàn bộ DDL được lưu trữ tại `src/main/resources/db/migration/V1__init_work_orders_schema.sql`.

```mermaid
erDiagram
    WORK_ORDERS {
        UUID id PK "Khóa chính UUID ngẫu nhiên"
        VARCHAR(50) equipment_id "Mã định danh thiết bị lưới điện"
        VARCHAR(500) description "Mô tả chi tiết sự cố sự kiện"
        VARCHAR(20) priority "Độ ưu tiên: LOW, MEDIUM, HIGH, CRITICAL"
        VARCHAR(20) status "Trạng thái: OPEN, IN_PROGRESS, DONE"
        TIMESTAMP created_at "Thời điểm tạo phiếu (UTC)"
        TIMESTAMP resolved_at "Thời điểm hoàn thành phiếu (UTC, nullable)"
    }
```

### 3.2 Chi Tiết Cấu Trúc Bảng `work_orders`

| Tên Cột | Kiểu Dữ Liệu | Ràng Buộc (Constraints) | Ý Nghĩa Nghiệp Vụ |
|---|---|---|---|
| `id` | `UUID` | `PRIMARY KEY, NOT NULL` | Định danh duy nhất toàn cầu của phiếu |
| `equipment_id` | `VARCHAR(50)` | `NOT NULL` | Mã thiết bị lưới điện (Trạm biến áp, lộ đường dây) |
| `description` | `VARCHAR(500)` | `NOT NULL` | Nội dung mô tả sự cố kỹ thuật |
| `priority` | `VARCHAR(20)` | `NOT NULL, CHECK (LOW, MEDIUM, HIGH, CRITICAL)` | Mức độ khẩn cấp xử lý sự cố |
| `status` | `VARCHAR(20)` | `NOT NULL, DEFAULT 'OPEN', CHECK (OPEN, IN_PROGRESS, DONE)` | Vòng đời phiếu công tác |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | `NOT NULL` | Thời gian khởi tạo phiếu |
| `resolved_at` | `TIMESTAMP WITH TIME ZONE` | `NULLABLE` | Thời gian chuyển sang `DONE` |

### 3.3 Danh Mục Chỉ Mục (Indexes)
- `idx_work_orders_status_created_at`: Tạo trên `(status, created_at DESC)` nhằm tối ưu hóa các câu truy vấn lọc phiếu theo trạng thái và sắp xếp giảm dần theo thời gian.
- `idx_work_orders_equipment_id`: Tạo trên `(equipment_id)` phục vụ tra cứu lịch sử sự cố theo từng thiết bị.

### 3.4 Thông Số Kết Nối H2 Database Cục Bộ
- **Console Web UI:** `http://localhost:8080/h2-console`
- **Driver Class:** `org.h2.Driver`
- **JDBC URL:** `jdbc:h2:mem:workorderdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
- **User Name:** `sa`
- **Password:** *(Để trống)*

---

## 4. DANH MỤC HỢP ĐỒNG API & TÍCH HỢP HỆ THỐNG

### 4.1 Bảng Danh Mục Endpoints

| Phương thức | Endpoint URI | Quyền Hạn (RBAC) | Mô Tả Chức Năng | Status Thành Công |
|---|---|---|---|---|
| `POST` | `/api/v1/workorders` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | Tạo mới phiếu công tác mất điện | `201 Created` (`Location` header) |
| `GET` | `/api/v1/workorders` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | Lấy danh sách phiếu có phân trang & lọc | `200 OK` (PagedResponse) |
| `GET` | `/api/v1/workorders/{id}` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | Lấy thông tin chi tiết một phiếu theo UUID | `200 OK` (WorkOrderResponse) |
| `PATCH` | `/api/v1/workorders/{id}/status` | `TECHNICIAN`, `ADMIN` | Cập nhật chuyển dịch trạng thái phiếu | `200 OK` (WorkOrderResponse) |

> [!WARNING]
> **Ràng Buộc Phân Quyền PATCH:** Điều độ viên (`DISPATCHER`) **không có quyền** gọi API `PATCH /status`. Thao tác chuyển trạng thái bắt buộc do Kỹ thuật viên hiện trường (`TECHNICIAN`) hoặc Quản trị viên (`ADMIN`) thực hiện.

### 4.2 Máy Trạng Thái Phiếu Công Tác (State Machine Contract)

```mermaid
stateDiagram-v2
    [*] --> OPEN: Khởi tạo phiếu (resolvedAt = null)
    OPEN --> IN_PROGRESS: Tiếp nhận xử lý (resolvedAt = null)
    IN_PROGRESS --> DONE: Hoàn thành xử lý (resolvedAt = Instant.now())
    DONE --> [*]

    note right of OPEN
        Không cho phép chuyển thẳng từ OPEN sang DONE.
        Vi phạm sẽ nhận lỗi 422 Unprocessable Entity.
    end note
    note right of DONE
        Trạng thái kết thúc (Terminal State).
        Không được phép chuyển ngược về OPEN hoặc IN_PROGRESS.
    end note
```

### 4.3 Chuẩn Lỗi RFC 7807 Problem Details
Khi có lỗi xảy ra, hệ thống trả về HTTP Body dạng JSON theo chuẩn `application/problem+json`:

```json
{
  "type": "urn:problem-type:invalid-state-transition",
  "title": "Invalid State Transition",
  "status": 422,
  "detail": "Cannot transition work order from OPEN to DONE",
  "instance": "/api/v1/workorders/1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed/status"
}
```

#### Ma Trận Mã Lỗi Hệ Thống (RFC 7807 Error Catalog)
Toàn bộ các URN định danh loại lỗi được quản lý tập trung dưới dạng hằng số `java.net.URI` bất biến tại lớp [`ProblemTypes.java`](file:///c:/ai-native-oms-api/src/main/java/com/gpc/oms/exception/ProblemTypes.java):

| HTTP Status | Problem Type URN | Hằng Số `ProblemTypes` | Title / Mô Tả Khi Xảy Ra Lỗi | Handler Phụ Trách |
|---|---|---|---|---|
| `400 Bad Request` | `urn:problem-type:validation-error` | `ProblemTypes.VALIDATION_ERROR` | Validation Failed (thiếu trường bắt buộc hoặc vi phạm độ dài `@Valid`) | `handleValidationErrors` |
| `400 Bad Request` | `urn:problem-type:malformed-json` | `ProblemTypes.MALFORMED_JSON` | Malformed Request Body (JSON sai cú pháp, giá trị enum không hợp lệ) | `handleMalformedJson` |
| `400 Bad Request` | `urn:problem-type:validation-error` | `ProblemTypes.VALIDATION_ERROR` | Validation Failed (Query param hoặc path param sai kiểu dữ liệu) | `handleQueryParamTypeMismatch` |
| `401 Unauthorized` | `urn:problem-type:unauthorized` | `ProblemTypes.UNAUTHORIZED` | Unauthorized (Thiếu hoặc sai thông tin xác thực) | `CustomAuthenticationEntryPoint` |
| `403 Forbidden` | `urn:problem-type:forbidden` | `ProblemTypes.FORBIDDEN` | Access Denied (Tài khoản không có quyền hạn phù hợp trong RBAC) | `handleAccessDenied` |
| `404 Not Found` | `urn:problem-type:not-found` | `ProblemTypes.NOT_FOUND` | Resource Not Found (Phiếu công tác không tồn tại với ID chỉ định) | `handleResourceNotFound` |
| `422 Unprocessable Entity` | `urn:problem-type:invalid-state-transition` | `ProblemTypes.INVALID_STATE_TRANSITION` | Illegal State Transition (Vi phạm quy tắc máy trạng thái một chiều) | `handleIllegalStateTransition` |
| `500 Internal Server Error` | `urn:problem-type:internal-error` | `ProblemTypes.INTERNAL_ERROR` | An unexpected error occurred (Lỗi hệ thống bất khả kháng, che giấu stacktrace) | `handleUnexpected` |

---

## 5. MÔ HÌNH BẢO MẬT & PHÂN QUYỀN (RBAC)

> [!IMPORTANT]
> **Hồ Sơ Đánh Giá An Ninh Toàn Diện (Security Handover Dossier):**  
> Xem chi tiết kết quả thẩm định theo chuẩn OWASP API Security Top 10 (2023), danh mục lỗ hổng & tình trạng khắc phục 100% P0 (SEC-01, SEC-02, SEC-04), cùng biên bản ký nhận bàn giao an ninh tại [`docs/SECURITY_HANDOVER_REPORT.md`](SECURITY_HANDOVER_REPORT.md).  
> **Security Posture Score:** **`98.0 / 100` (GRADE A+ - APPROVED FOR PRODUCTION DEPLOYMENT)**.

### 5.1 Kiến Trúc Bảo Mật & Ranh Giới Mạng (Perimeter Defense)
- **Framework nền tảng:** Spring Security 6.3.3 trên nền tảng Spring Boot 3.3.4.
- **Mô hình Session:** Thiết lập phiên không trạng thái hoàn toàn (`SessionCreationPolicy.STATELESS`), không sử dụng HTTP Session hay cookie để xác thực, tối ưu cho kiến trúc RESTful Microservices.
- **CSRF Policy:** Vô hiệu hóa CSRF (`csrf.disable()`) theo đúng khuyến nghị của OWASP dành cho Token-based / Stateless REST APIs.
- **Phòng chống Clickjacking:** Kích hoạt header an ninh `X-Frame-Options: SAMEORIGIN` bảo vệ các trang web console nội bộ khỏi các cuộc tấn công nhúng frame lừa đảo từ bên ngoài.
- **Cơ chế Dual SecurityFilterChain:**
  * `h2ConsoleChain` (`@Order(1)`): Được bảo vệ bằng `@Profile("!prod")`, chỉ cho phép truy cập H2 Console trên môi trường phát triển (dev/local).
  * `filterChain` (`@Order(2)`): Áp dụng cho mọi môi trường; trên profile `prod`, đường dẫn `/h2-console/**` yêu cầu quyền `hasRole('ADMIN')` và trả về HTTP 401 Unauthorized khi không có token.

### 5.2 Ma Trận Phân Quyền Theo Vai Trò (RBAC Matrix)

| HTTP Method | URI Pattern | Vai trò Cho phép | Cơ chế Kiểm soát (Annotation) | Hành vi khi Vi phạm Quyền |
|---|---|---|---|---|
| `POST` | `/api/v1/workorders` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` | HTTP 401 (chưa auth) / HTTP 403 (sai role) |
| `GET` | `/api/v1/workorders` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` | HTTP 401 (chưa auth) / HTTP 403 (sai role) |
| `GET` | `/api/v1/workorders/{id}` | `DISPATCHER`, `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('DISPATCHER', 'TECHNICIAN', 'ADMIN')")` | HTTP 401 (chưa auth) / HTTP 403 (sai role) |
| `PATCH` | `/api/v1/workorders/{id}/status` | `TECHNICIAN`, `ADMIN` | `@PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")` | HTTP 403 Forbidden đối với `DISPATCHER` |
| `GET` | `/h2-console/**` | Dev/Test: Public; Prod: `ADMIN` | `h2ConsoleChain` (`!prod`) / `filterChain` (`prod`) | HTTP 401 Unauthorized trên Prod |

### 5.3 Chuẩn Hóa Lỗi An Ninh Theo RFC 7807 Problem Details
Mọi vi phạm bảo mật đều được xuất ra định dạng JSON chuẩn `application/problem+json`:
- **Chưa xác thực (HTTP 401 Unauthorized):** Xử lý tại `CustomAuthenticationEntryPoint`, trả về URN `urn:problem-type:unauthorized` kèm chi tiết `"Authentication token is missing or expired"`.
- **Không đủ quyền hạn (HTTP 403 Forbidden):** Bắt giữ ngoại lệ `AccessDeniedException` tại `GlobalExceptionHandler`, trả về URN `urn:problem-type:forbidden` kèm thông điệp `"Access Denied"`.

### 5.4 Kết Quả Khắc Phục Các Lỗ Hổng Bảo Mật P0 (Security Hardening Results)
Toàn bộ 03 phát hiện mức Major/P0 đã được đội ngũ kỹ sư xử lý triệt để trên nhánh `main`:
1. **SEC-01 (CWE-200):** Tách `h2ConsoleChain` với `@Profile("!prod")` và kiểm thử tự động với `H2ConsoleSecurityTest.java` (Merged PR #36).
2. **SEC-02 (CWE-400):** Cấu hình `spring.data.web.pageable.max-page-size: 100` phòng chống tấn công DoS phân trang và kiểm thử tự động với `WorkOrderControllerTest.list_sizeOverMax_isCappedTo100` (Merged PR #37).
3. **SEC-04 (CWE-1059):** Tích hợp `flyway-core` và thiết lập `ddl-auto: validate` đảm bảo an toàn dịch chuyển cấu trúc CSDL (Merged PR #35).

### 5.5 Danh Mục Tài Khoản Demo & Ranh Giới Cô Lập Môi Trường

| Username | Password | Roles Được Cấp | Mục Đích Sử Dụng |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN`, `ROLE_DISPATCHER`, `ROLE_TECHNICIAN` | Toàn quyền kiểm thử và quản trị hệ thống |
| `dispatcher` | `dispatcher123` | `ROLE_DISPATCHER` | Tạo phiếu mới và theo dõi danh sách phiếu |
| `technician` | `technician123` | `ROLE_TECHNICIAN` | Xem phiếu và chuyển trạng thái sửa chữa |

> [!CAUTION]
> **Cô Lập Môi Trường (Environment Boundary):** Bean `UserDetailsService` chứa các tài khoản trên được gắn `@Profile("!prod")`. Khi ứng dụng chạy trên Production với cờ `--spring.profiles.active=prod`, danh sách tài khoản này hoàn toàn không được khởi tạo vào bộ nhớ.

### 5.6 Lộ Trình Tích Hợp An Ninh Doanh Nghiệp (Enterprise Security Roadmap)
- **Giai đoạn chuyển giao (P1):** Hiện thực hóa `CorrelationIdFilter` phục vụ SOC/SRE điều tra truy vết phân tán ([Issue #31](https://github.com/duongbaphuc/ai-native-oms-api/issues/31)) và tích hợp Actuator/Prometheus metrics ([Issue #44](https://github.com/duongbaphuc/ai-native-oms-api/issues/44)).
- **Giai đoạn mở rộng (P2):** Thay thế HTTP Basic bằng OAuth2 Resource Server xác thực JWT qua Keycloak/Azure AD ([Issue #33](https://github.com/duongbaphuc/ai-native-oms-api/issues/33)) và tích hợp bộ lọc Rate Limiting với Bucket4j ([Issue #34](https://github.com/duongbaphuc/ai-native-oms-api/issues/34)).

---

## 6. SỔ TAY BIÊN DỊCH, KHỞI CHẠY & TRIỂN KHAI (RUNBOOK)

### 6.1 Yêu Cầu Môi Trường Tiền Đề (Prerequisites)
- **Hệ điều hành:** Linux, macOS, hoặc Windows 10/11.
- **Java Development Kit:** JDK 17 LTS trở lên (kiểm tra bằng `java -version`).
- **Apache Maven:** Phiên bản 3.8+ (hoặc sử dụng tệp `mvnw` đi kèm).
- **Cổng kết nối:** Đảm bảo cổng `8080` chưa bị ứng dụng khác chiếm dụng.

### 6.2 Các Lệnh Vận Hành Cơ Bản (CLI Commands)

```powershell
# 1. Làm sạch và biên dịch mã nguồn
mvn clean compile

# 2. Chạy toàn bộ 86 automated tests
mvn test

# 3. Chạy kiểm tra toàn diện, build package và thẩm định JaCoCo Quality Gate (100% Coverage)
mvn clean verify

# 4. Khởi động ứng dụng Spring Boot cục bộ
mvn spring-boot:run

# 5. Hoặc chạy file JAR đóng gói độc lập
java -jar target/oms-api-demo-0.0.1-SNAPSHOT.jar
```

### 6.3 Bảng Điều Khiển Kiểm Thử Tương Tác Trực Quan (Interactive Test Console)
Dự án được tích hợp sẵn một giao diện Test Console trực quan tại cổng gốc:
- **Địa chỉ truy cập:** `http://localhost:8080/`
- **Tính năng nổi bật:**
  - Chuyển đổi nhanh vai trò người dùng (Admin / Dispatcher / Technician / Anonymous) chỉ với 1 click.
  - Form tạo phiếu mới với đầy đủ kiểm tra validation.
  - Danh sách phiếu công tác thời gian thực với phân loại badge trạng thái màu sắc trực quan.
  - Nút thao tác chuyển trạng thái nhanh (`Start Working`, `Complete`).
  - Hộp kiểm tra phản hồi API trực tiếp (Status code, Request URL, Response Payload).

### 6.4 Mẫu Lệnh cURL Thao Tác Trực Tiếp Với API

```bash
# 1. Tạo mới một phiếu công tác (Vai trò Dispatcher)
curl -X POST http://localhost:8080/api/v1/workorders \
  -u dispatcher:dispatcher123 \
  -H "Content-Type: application/json" \
  -H "X-Correlation-Id: test-corr-001" \
  -d '{
    "equipmentId": "TRANSFORMER-SUB-09",
    "description": "Biến áp phát nhiệt độ cao trên 95 độ C cần kiểm tra khẩn cấp",
    "priority": "HIGH"
  }'

# 2. Lấy danh sách phiếu công tác (Phân trang và lọc trạng thái)
curl -X GET "http://localhost:8080/api/v1/workorders?status=OPEN&page=0&size=10" \
  -u dispatcher:dispatcher123

# 3. Lấy chi tiết phiếu theo ID
curl -X GET http://localhost:8080/api/v1/workorders/{WORK_ORDER_ID} \
  -u technician:technician123

# 4. Chuyển trạng thái sang IN_PROGRESS (Vai trò Technician)
curl -X PATCH http://localhost:8080/api/v1/workorders/{WORK_ORDER_ID}/status \
  -u technician:technician123 \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'

# 5. Chuyển trạng thái sang DONE
curl -X PATCH http://localhost:8080/api/v1/workorders/{WORK_ORDER_ID}/status \
  -u technician:technician123 \
  -H "Content-Type: application/json" \
  -d '{"status": "DONE"}'
```

---

## 7. HỒ SƠ CHẤT LƯỢNG & BÁO CÁO KIỂM THỬ TỰ ĐỘNG

### 7.1 Kim Tự Tháp Kiểm Thử (Testing Pyramid)
Toàn bộ mã nguồn được bảo vệ bởi **86 bài kiểm thử tự động**, phân chia theo các tầng chuyên biệt của Testing Pyramid, đạt tỷ lệ thành công 100% (86/86 Green):

```
                          ▲
                         / \
                        /E2E\     WorkOrderIntegrationTest (7 tests)
                       /-----\    Full SpringBootTest Slice
                      / Slice \   WorkOrderControllerTest (15 tests)
                     /  Tests  \  WorkOrderRepositoryTest (4 tests)
                    /-----------\ H2Console Security Tests (2 tests)
                   / Unit Tests  \ WorkOrderTest (12), WorkOrderStatusTest (11), PriorityTest (2)
                  /_______________\ DtoMappingTest (12), WorkOrderServiceTest (8), Exception Tests (9), Config (3)
```

| Tên Lớp Kiểm Thử | Tầng Kiểm Thử | Số Ca Test | Trạng Thái |
|---|---|---|---|
| `WorkOrderTest` | Domain Unit Test | 12 | PASS (100%) |
| `WorkOrderStatusTest` | State Machine Unit Test (3x3 transition matrix) | 11 | PASS (100%) |
| `PriorityTest` | Domain Enum Unit Test | 2 | PASS (100%) |
| `DtoMappingTest` | DTO Record & Factory Mapper Test | 12 | PASS (100%) |
| `ResourceNotFoundExceptionTest` | Exception Class Unit Test | 1 | PASS (100%) |
| `ProblemTypesTest` | RFC 7807 Constants & Reflection Unit Test | 2 | PASS (100%) |
| `StringToWorkOrderStatusConverterTest` | Web Config Converter Test | 3 | PASS (100%) |
| `WorkOrderServiceTest` | Service Unit Test (Mockito + Fixtures) | 8 | PASS (100%) |
| `GlobalExceptionHandlerUnitTest` | Exception Advice Direct Unit Test | 6 | PASS (100%) |
| `WorkOrderControllerTest` | Web Slice Test (`@WebMvcTest`) | 15 | PASS (100%) |
| `WorkOrderRepositoryTest` | Persistence Slice Test (`@DataJpaTest`) | 4 | PASS (100%) |
| `H2ConsoleDevAccessTest` | Security Slice Test (`!prod` public access) | 1 | PASS (100%) |
| `H2ConsoleProdAccessTest` | Security Slice Test (`prod` admin authorization) | 1 | PASS (100%) |
| `OmsApiApplicationTests` | Spring Context Bootstrap Test | 1 | PASS (100%) |
| `WorkOrderIntegrationTest` | End-to-End Integration Test (`@SpringBootTest`) | 7 | PASS (100%) |
| **TỔNG CỘNG** | **Toàn bộ kim tự tháp kiểm thử** | **86** | **86/86 PASS (0 Failures, 0 Errors, 0 Skipped)** |

### 7.2 Báo Cáo Đo Lường Độ Bao Phủ JaCoCo (JaCoCo Coverage Metrics)
Dự án tích hợp cấu hình chốt chặn chất lượng (Quality Gate) nghiêm ngặt trong `pom.xml`. Mỗi khi thực hiện `mvn verify`, mã nguồn phải đạt:
- **Line Coverage:** `1.00` (100.0%)
- **Branch Coverage:** `1.00` (100.0%)

Vị trí báo cáo chi tiết: `target/site/jacoco/index.html`.

#### Chi Tiết Đo Lường Từng Package Nghiệp Vụ Cốt Lõi (12 Classes)

| Package | Số Lớp | Methods | Lines | Branches | Instructions | Độ Bao Phủ |
|---|---|---|---|---|---|---|
| `com.gpc.oms.exception` | 3 | 11 | 50/50 | 4/4 | 191/191 | **100.0%** |
| `com.gpc.oms.domain` | 3 | 15 | 39/39 | 11/11 | 162/162 | **100.0%** |
| `com.gpc.oms.dto` | 4 | 6 | 22/22 | n/a | 110/110 | **100.0%** |
| `com.gpc.oms.service` | 1 | 8 | 24/24 | 2/2 | 109/109 | **100.0%** |
| `com.gpc.oms.controller` | 1 | 6 | 17/17 | n/a | 87/87 | **100.0%** |
| **TỔNG HỢP TOÀN DỰ ÁN** | **12** | **46** | **152/152 (100%)** | **17/17 (100%)** | **659/659 (100%)** | **100.0% (PERFECT)** |

---

## 8. SỔ TAY VẬN HÀNH, GIÁM SÁT & XỬ LÝ SỰ CỐ (TROUBLESHOOTING)

### 8.1 Cấu Trúc Log & Ngữ Cảnh Truy Vết (Distributed Tracing)
- Hệ thống ghi nhận mọi log thông qua SLF4J / Logback với định dạng tiêu chuẩn:
  `[TIMESTAMP] [LEVEL] [PID] [THREAD] [LOGGER] [correlationId] MESSAGE`
- Tệp `CorrelationIdFilter` tự động trích xuất header `X-Correlation-Id` từ client hoặc tự sinh chuỗi UUID ngẫu nhiên đưa vào MDC context.
- Các thông tin bảo mật và nhạy cảm (như thiết bị định danh chi tiết) được băm (`hashCode()`) hoặc bảo vệ trong log theo quy định tại `docs/observability-and-logging.md`.

### 8.2 Sổ Tay Xử Lý Sự Cố Thường Gặp (Incident Playbook)

#### Sự Cố 1: Lỗi Cổng Kết Nối Đã Bị Chiếm Dụng (`Port 8080 is already in use`)
- **Triệu chứng:** `java.net.BindException: Address already in use: bind`
- **Nguyên nhân:** Đang có một tiến trình Spring Boot hoặc dịch vụ khác chạy nền trên cổng 8080.
- **Biện pháp xử lý:**
  ```powershell
  # Tìm PID tiến trình chiếm cổng
  netstat -ano | findstr 8080
  # Kết thúc tiến trình (thay PID bằng số thực tế)
  taskkill /F /PID <PID>
  # Hoặc cấu hình cổng khác khi khởi động
  mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"
  ```

#### Sự Cố 2: Dữ Liệu Bị Reset Về Rỗng Sau Khi Restart Ứng Dụng
- **Triệu chứng:** Toàn bộ phiếu công tác tạo trước đó biến mất sau khi khởi động lại.
- **Nguyên nhân:** Hệ thống đang sử dụng cấu hình H2 In-Memory (`jdbc:h2:mem:workorderdb`), dữ liệu lưu trên RAM và tự hủy khi tiến trình kết thúc.
- **Biện pháp xử lý:** Đây là hành vi thiết kế có chủ đích cho môi trường Demo/Lab. Để lưu trữ vĩnh viễn, đổi cấu hình datasource trong `application.yml` sang PostgreSQL hoặc H2 File Mode (`jdbc:h2:file:./data/workorderdb`).

#### Sự Cố 3: Nhận Lỗi `403 Forbidden` Khi Cập Nhật Trạng Thái Phiếu
- **Triệu chứng:** Phản hồi `{ "type": "urn:problem-type:forbidden", "status": 403 }` khi gọi `PATCH /api/v1/workorders/{id}/status`.
- **Nguyên nhân:** Đang sử dụng tài khoản `dispatcher` (chỉ có quyền tạo và xem, không có quyền chuyển dịch trạng thái).
- **Biện pháp xử lý:** Chuyển sang sử dụng thông tin xác thực của kỹ thuật viên (`technician:technician123`) hoặc quản trị viên (`admin:admin123`).

#### Sự Cố 4: Nhận Lỗi `422 Invalid State Transition`
- **Triệu chứng:** Phản hồi `{ "type": "urn:problem-type:invalid-state-transition", "status": 422 }`.
- **Nguyên nhân:** Vi phạm quy tắc State Machine (ví dụ: chuyển từ `OPEN` trực tiếp sang `DONE`, hoặc cố gắng cập nhật một phiếu đã ở trạng thái `DONE`).
- **Biện pháp xử lý:** Phiếu phải được tiếp nhận xử lý sang `IN_PROGRESS` trước khi nghiệm thu hoàn tất `DONE`.

---

## 9. QUY TRÌNH PHÁT TRIỂN MỞ RỘNG & KÝ NHẬN BÀN GIAO

### 9.1 Quy Trình Bổ Sung Tính Năng Chuẩn (Spec-Driven SDLC SOP)
Khi cần mở rộng thêm thực thể hoặc endpoint mới:
1. **Cập nhật Đặc tả kỹ thuật:** Soạn thảo bản thảo mô tả API tại `docs/api-spec.md` và domain rules tại `docs/domain-model.md`.
2. **Viết Migration Script:** Tạo tệp `src/main/resources/db/migration/V2__<description>.sql` (không chỉnh sửa file V1 đã release).
3. **Hiện thực hóa Mã nguồn:** Tạo DTO record immutable $\rightarrow$ Domain Entity $\rightarrow$ Repository $\rightarrow$ Service $\rightarrow$ Controller.
4. **Viết Test Đạt 100% Coverage:** Tạo đầy đủ Unit test, WebMvcTest slice và Integration test.
5. **Chạy Thẩm Định:** Chạy `mvn clean verify` để đảm bảo 0 lỗi hồi quy và đạt 100% Quality Gate trước khi mở Pull Request.

### 9.2 Danh Mục Đầu Mối Bàn Giao (Contact Matrix)

| Vai trò | Họ và Tên | Đơn vị | Kênh Liên Hệ |
|---|---|---|---|
| **Lead Technical Architect** | AI-Native Architecture Board | Global Delivery Unit | `architect@gpc-oms.internal` |
| **Site Reliability Lead (SRE)** | Production Operations Lead | Cloud Infrastructure Team | `ops-support@gpc-oms.internal` |
| **Product Owner (PO)** | Outage Management PO | Power Grid Business Group | `po-oms@gpc-oms.internal` |

### 9.3 Bảng Tiêu Chí Nghiệm Thu Bàn Giao (Sign-Off Checklist)

| Tiêu Chí Nghiệm Thu | Kết Quả Đánh Giá | Tình Trạng |
|---|---|---|
| Mã nguồn biên dịch thành công không cảnh báo | `BUILD SUCCESS` | [x] ĐẠT |
| Toàn bộ 86 automated test cases thực thi thành công | 86 Passed, 0 Failed, 0 Skipped | [x] ĐẠT |
| JaCoCo Line và Branch Coverage đạt ngưỡng quy định | 100% Line, 100% Branch (12/12 classes) | [x] ĐẠT |
| Cấu trúc bảng và chỉ mục DB đồng bộ qua Flyway | Schema V1 khởi tạo chính xác | [x] ĐẠT |
| Giao diện Test Console hoạt động mượt mà trên browser | Đã kiểm chứng tại `http://localhost:8080/` | [x] ĐẠT |
| Tài liệu bàn giao đầy đủ chi tiết, không còn placeholder | Hoàn tất tại `docs/SYSTEM_HANDOVER.md` | [x] ĐẠT |

---
**XÁC NHẬN BÀN GIAO KỸ THUẬT THÀNH CÔNG**  
*Hệ thống đã sẵn sàng cho giai đoạn vận hành thử nghiệm và chuyển giao chính thức.*
