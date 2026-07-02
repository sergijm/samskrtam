FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew :services:sangraha-service:bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S samskrtam && adduser -S samskrtam -G samskrtam
USER samskrtam
WORKDIR /app
COPY --from=builder /app/services/sangraha-service/build/libs/*.jar app.jar
EXPOSE ${SANGRAHA_SERVICE_PORT}
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget -qO- http://localhost:${MANAGEMENT_PORT}/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]