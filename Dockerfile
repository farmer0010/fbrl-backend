# syntax=docker/dockerfile:1

# --- Build stage ---
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Gradle wrapper와 빌드 스크립트를 먼저 복사해서 의존성 다운로드 레이어를 캐싱
COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 소스 복사 후 실제 빌드 (테스트는 별도 CI에서 이미 검증되므로 이미지 빌드에서는 생략)
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
