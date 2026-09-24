# ==========================================
# STAGE 1: Build & Package (Maven + JDK 17 Alpine)
# ==========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /workspace

# Tận dụng cơ chế layer cache của Docker cho dependencies
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copy mã nguồn và đóng gói JAR ứng dụng
COPY src ./src
RUN mvn clean package -DskipTests -B

# ==========================================
# STAGE 2: Lightweight Hardened Runtime (JRE 17 Alpine)
# ==========================================
FROM eclipse-temurin:17-jre-alpine AS runner

LABEL maintainer="GPC Outage Architecture Team <architect@gpc-oms.internal>" \
      service="ai-native-oms-api" \
      version="1.0.0-RELEASE"

WORKDIR /app

# Gia cố bảo mật: Tạo group và user phi đặc quyền (UID/GID 10001)
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup

# Copy file JAR đã đóng gói từ stage builder
COPY --from=builder --chown=appuser:appgroup /workspace/target/*.jar /app/app.jar

# Chuyển sang non-root user (Cấm tuyệt đối chạy container dưới quyền root)
USER 10001:10001

# Tối ưu hóa JVM cho môi trường Container (Memory limits, entropy source, UTC)
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom -Duser.timezone=UTC"

EXPOSE 8080

# Thăm dò sức khỏe (Healthcheck probe) qua Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
