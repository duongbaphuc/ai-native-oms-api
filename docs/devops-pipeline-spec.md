<!--
Role: Principal Cloud DevOps & Platform Architect
Task: Multi-Stage Containerization, JVM Container Tuning, GitHub Actions CI Pipeline, and Secret Governance
Context files: docs/coding-rules.md, docs/security-rules.md, docs/ADR-001-use-h2-database.md
Constraints: Multi-stage Dockerfile, Non-root user (appuser:10001), JaCoCo >= 80% Quality Gate, Trivy CVE scan
-->
# Containerization & CI/CD Pipeline Blueprint

Tài liệu này xác định bản vẽ kỹ thuật đóng gói Container hóa và thiết kế quy trình Tích hợp liên tục / Triển khai liên tục (CI/CD) cho microservice Outage Work Order API.

Tài liệu là cơ sở ngữ cảnh vững chắc để GitHub Copilot tự động sinh `Dockerfile`, `.dockerignore`, file cấu hình GitHub Actions Workflow (`.github/workflows/ci.yml`), và quản trị biến môi trường trong môi trường đám mây Kubernetes / Container Runtime.

---

## 1. Chuẩn mực Đóng gói Container (Multi-Stage Dockerfile)

Để tối ưu hóa dung lượng image và bảo mật môi trường thực thi:
1. **Multi-Stage Build:** Tách biệt tuyệt đối giữa môi trường biên dịch (chứa JDK, Maven, source code) và môi trường vận hành (chỉ chứa JRE tối giản và file JAR đã build).
2. **Nguyên tắc Đặc quyền Tối thiểu (Non-Root User):** Cấm chạy ứng dụng dưới quyền `root`. Bắt buộc tạo user hệ thống phi đặc quyền `appuser` (UID: 10001).
3. **Tối ưu hóa JVM cho Container:** Cấu hình để JVM nhận biết chính xác giới hạn tài nguyên của cgroups (Memory & CPU).

### Mã nguồn Chuẩn: `Dockerfile`

```dockerfile
# ==========================================
# STAGE 1: Build & Package (Maven + JDK 17)
# ==========================================
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

# Tận dụng cơ chế layer cache của Docker cho dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy mã nguồn và đóng gói
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Trích xuất Spring Boot Layered JAR để tối ưu hóa cache khi deploy
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted/

# ==========================================
# STAGE 2: Lightweight Runtime (JRE 17)
# ==========================================
FROM eclipse-temurin:17-jre-jammy AS runner

LABEL maintainer="GPC Outage Team <dev@gpc.com>"
LABEL service="ai-native-oms-api"

WORKDIR /app

# Tạo group và user phi đặc quyền (UID 10001)
RUN groupadd -g 10001 appuser && \
    useradd -u 10001 -g appuser -m -s /bin/bash appuser

# Copy các layer đã được trích xuất từ stage builder
COPY --from=builder --chown=appuser:appuser /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=appuser:appuser /workspace/extracted/application/ ./

# Chuyển sang non-root user
USER appuser:appuser

# Cấu hình cờ JVM tối ưu hóa bộ nhớ cho Container Runtime
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Duser.timezone=UTC"

EXPOSE 8080

# Healthcheck định kỳ qua Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
```

---

## 2. Quy chuẩn Tệp `.dockerignore`

Ngăn chặn việc đưa các tệp không cần thiết hoặc chứa dữ liệu nhạy cảm vào Docker build context:

```text
# Git & Metadata
.git
.gitignore
.gitattributes

# IDE & Editor files
.idea
.vscode
*.iml
*.swp

# Build & Test Artifacts
target/
*.log
.mvn/wrapper/maven-wrapper.jar

# Secrets & Environment variables
*.env
*.pem
*.key
*.p12

# Documentation
docs/
*.md
```

---

## 3. Bản thiết kế Quy trình CI/CD (GitHub Actions Workflow)

Tệp workflow `.github/workflows/ci.yml` kiểm soát chất lượng mã nguồn tự động trước khi cho phép merge vào nhánh `main`.

### Bảng Các Giai đoạn Kiểm định (Quality Gates)

| Giai đoạn (Job) | Nhiệm vụ chính | Điều kiện Đạt (Pass Criteria) |
|---|---|---|
| **`lint-and-compile`** | Kiểm tra cú pháp, format mã nguồn, biên dịch Java 17 | Không có cảnh báo lỗi cú pháp |
| **`unit-and-integration-test`** | Chạy toàn bộ test suite JUnit 5 + MockMvc | 100% tests Passed, không flaky |
| **`code-coverage-gate`** | Xuất báo cáo đo độ bao phủ JaCoCo | Line coverage $\ge$ 80%, Branch coverage $\ge$ 75% |
| **`vulnerability-scan`** | Sử dụng Trivy scan mã nguồn và dependencies | 0 CVE mức `CRITICAL`, tối đa 0 CVE mức `HIGH` |
| **`container-dry-run`** | Thử nghiệm build Docker image theo multi-stage | Docker build thành công |

### Mã nguồn: `.github/workflows/ci.yml`

```yaml
name: AI-Native SDLC CI Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

permissions:
  contents: read
  security-events: write

jobs:
  build-and-test:
    name: Build, Test & Coverage Gate
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Setup Java 17 (Temurin)
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Compile and Run Tests
        run: ./mvnw clean verify -B

      - name: Validate JaCoCo Coverage Threshold (>= 80%)
        run: |
          COVERAGE=$(awk -F"," '{ instructions += $4 + $5; covered += $5 } END { print int(100*covered/instructions) }' target/site/jacoco/jacoco.csv)
          echo "Current Code Coverage: $COVERAGE%"
          if [ "$COVERAGE" -lt 80 ]; then
            echo "ERROR: Code coverage is below threshold (80%). Current: $COVERAGE%"
            exit 1
          fi

      - name: Archive Test Results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: surefire-reports
          path: target/surefire-reports/

  security-scan:
    name: Dependency & Container Vulnerability Scan
    runs-on: ubuntu-latest
    needs: build-and-test
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Run Trivy Vulnerability Scanner (Repo & Deps)
        uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          ignore-unfixed: true
          severity: 'CRITICAL,HIGH'
          exit-code: '1'
```

---

## 4. Quản trị Biến Môi trường & Bí mật (Secret Governance)

### Bảng Ma trận Quản lý Cấu hình theo Môi trường

| Tên Biến Môi trường | Môi trường Local (Dev) | Môi trường Staging | Môi trường Production | Là Secret? |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `h2` | `postgres-staging` | `postgres-prod` | Không |
| `SPRING_DATASOURCE_URL` | `jdbc:h2:mem:workorderdb` | `jdbc:postgresql://stg-db:5432/oms` | `jdbc:postgresql://prod-db:5432/oms` | Không |
| `SPRING_DATASOURCE_USERNAME` | `sa` | `oms_app` | `oms_app` | Không |
| `SPRING_DATASOURCE_PASSWORD` | `""` | Khai báo qua Secret | Khai báo qua Secret | **CÓ** |
| `JWT_PUBLIC_KEY` | Mock Public Key | Staging Key Store | HSM / KMS Vault Key | **CÓ** |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | `https://stg-oms.gpc.com` | `https://oms.gpc.com` | Không |
| `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE` | `health,info,prometheus` | `health,info,prometheus` | `health,info,prometheus` | Không |

> [!CAUTION]
> Tuyệt đối không lưu trữ các biến có gắn nhãn **Là Secret = CÓ** vào Git repository, file markdown, hoặc file `application.yml` mặc định. Các giá trị này phải được nạp thông qua **Kubernetes Secrets**, **HashiCorp Vault**, hoặc **GitHub Actions Encrypted Secrets**.
