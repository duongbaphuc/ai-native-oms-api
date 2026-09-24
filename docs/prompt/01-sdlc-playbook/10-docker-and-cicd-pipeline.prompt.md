# Prompt Giai Đoạn 10: Đóng Gói Container, Docker Compose & Thiết Lập Đường Ống CI/CD Tự Động (DevOps Pipeline)

```markdown
# ROLE:
Bạn là một "Lead DevSecOps Architect" kiêm "Cloud-Native Platform Engineer" với hơn 15 năm kinh nghiệm về Containerization, Kubernetes-ready Microservices, Hardened Docker Images và Tự động hóa CI/CD Pipelines (GitHub Actions / GitLab CI) cho các hệ thống phần mềm tài chính và công nghiệp trọng yếu. Mục tiêu tối thượng của bạn là hiện thực hóa toàn bộ tài liệu đặc tả `docs/devops-pipeline-spec.md`, biến ứng dụng Java 17 Spring Boot 3.3 thành một gói container siêu nhẹ, bảo mật cao và được bảo vệ bởi đường ống tự động kiểm định chất lượng nghiêm ngặt trước khi triển khai.

---

# TASK:
Hiện thực hóa toàn diện hạ tầng đóng gói và đường ống phân phối liên tục (CI/CD) cho dự án `ai-native-oms-api` theo 4 hạng mục kỹ thuật cốt lõi:

1. **Xây Dựng Dockerfile Đa Tầng Tối Ưu & Bảo Mật (Multi-Stage Production Dockerfile):**
   - Tạo tệp `Dockerfile` tại thư mục gốc với kiến trúc 2 giai đoạn (Multi-stage build):
     * **Giai đoạn 1 (`builder`):** Sử dụng image `maven:3.9-eclipse-temurin-17-alpine`, tận dụng Docker layer caching bằng cách copy riêng `pom.xml` để tải dependencies trước (`mvn dependency:go-offline`), sau đó mới copy `src/` và đóng gói (`mvn clean package -DskipTests`).
     * **Giai đoạn 2 (`runner`):** Sử dụng base image JRE siêu nhẹ và an toàn `eclipse-temurin:17-jre-alpine` (hoặc Distroless).
   - **Gia Cố Bảo Mật Container (Container Hardening):**
     * Tạo user và group riêng biệt không có quyền root (`appuser:appgroup` với UID/GID `10001`). Tuyệt đối cấm chạy container dưới quyền `root`.
     * Tối ưu hóa JVM cho môi trường Container: Cấu hình `JAVA_TOOL_OPTIONS` với `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`, `-Djava.security.egd=file:/dev/./urandom`.
   - **Tích Hợp Thăm Dò Sức Khỏe (Healthcheck Probe):**
     * Cấu hình chỉ thị `HEALTHCHECK --interval=30s --timeout=5s --retries=3 CMD wget -qO- http://localhost:8080/actuator/health || exit 1`.

2. **Cấu Hình Môi Trường Chạy Tích Hợp Cục Bộ (`docker-compose.yml`):**
   - Tạo tệp `docker-compose.yml` hỗ trợ kiểm thử tích hợp môi trường giống Production:
     * Dịch vụ `oms-api`: Build từ Dockerfile cục bộ, ánh xạ cổng `8080:8080`, nạp biến môi trường datasource kết nối tới PostgreSQL, phụ thuộc vào service `postgres` (`depends_on` với điều kiện `service_healthy`).
     * Dịch vụ `postgres`: Image `postgres:15-alpine`, cấu hình persistent volume `pgdata`, thiết lập `POSTGRES_DB=workorderdb`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres_dev_only`, kèm script healthcheck `pg_isready`.
     * Mạng nội bộ biệt lập: Khai báo bridge network `oms-network`.

3. **Thiết Lập Đường Ống CI/CD Tự Động (`.github/workflows/ci.yml`):**
   - Tạo file cấu hình GitHub Actions Workflow tại `.github/workflows/ci.yml`:
     * Kích hoạt tự động khi: `push` vào nhánh `main` hoặc mở `pull_request` vào `main`.
     * **Job 1 (`build-and-test`):**
       - Khởi tạo môi trường Ubuntu runner, cài đặt JDK 17 (Temurin).
       - Chạy `mvn clean verify` kiểm tra toàn bộ 75 tests tự động.
       - Thẩm định chốt chặn **JaCoCo Quality Gate**: Tự động fail build nếu Line Coverage hoặc Branch Coverage dưới ngưỡng 100% trên các gói nghiệp vụ.
       - Upload báo cáo kiểm thử và artifact `jacoco-report` lưu trữ.
     * **Job 2 (`security-scan`):**
       - Quét mã nguồn và container image bằng công cụ bảo mật (Trivy Action).
       - Chặn đứng quy trình merge nếu phát hiện lỗ hổng nghiêm trọng mức `CRITICAL` hoặc `HIGH` (CVEs).
     * **Job 3 (`docker-build`):**
       - Kiểm tra tính hợp lệ của Dockerfile bằng Hadolint.
       - Thực hiện `docker build` để xác nhận image đóng gói thành công mà không có lỗi.

4. **Tích Hợp Giám Sát Khả Dụng (Spring Boot Actuator):**
   - Đảm bảo dependency `spring-boot-starter-actuator` được khai báo trong `pom.xml`.
   - Cấu hình `src/main/resources/application.yml` mở endpoint `/actuator/health` và `/actuator/info` cho Docker/Kubernetes Liveness & Readiness Probes, đồng thời ẩn toàn bộ các endpoint nhạy cảm (`env`, `beans`, `mappings`).

---

# CONSTRAINTS:
1. **Cấm Chạy Root Trong Container:** Container bắt buộc phải chuyển sang user không đặc quyền (`USER 10001:10001`) trước chỉ thị `ENTRYPOINT`.
2. **Kích Thước Image Tối Ưu:** Image thành phẩm ở giai đoạn runner không được vượt quá 200MB. Không giữ lại mã nguồn, Maven cache hay build tools trong image cuối.
3. **Tính Độc Lập Hoàn Toàn Của CI:** Workflow trên GitHub Actions phải tự túc toàn bộ dependencies và chạy được trên clean runner, không phụ thuộc vào bất kỳ file cấu hình máy trạm local nào.
4. **Vệ Sinh Secret Trong File Compose:** Mọi mật khẩu trong `docker-compose.yml` phải được ghi chú rõ ràng là chỉ dành cho môi trường Dev/Lab cục bộ, sẵn sàng thay thế bằng `.env` hoặc GitHub Secrets.

---

# DONE WHEN:
1. Tệp `Dockerfile`, `docker-compose.yml`, và `.github/workflows/ci.yml` được tạo đầy đủ đúng vị trí.
2. Lệnh `docker build -t oms-api-demo:latest .` biên dịch và đóng gói thành công image không phát sinh cảnh báo bảo mật.
3. Lệnh `docker compose up -d` khởi động thành công cụm ứng dụng và cơ sở dữ liệu.
4. Lệnh `curl http://localhost:8080/actuator/health` trả về `{"status":"UP"}`.
5. GitHub Actions workflow hợp lệ cú pháp (`action-validator`) và sẵn sàng kích hoạt tự động trên remote repository.
```
