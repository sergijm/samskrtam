plugins {
    alias(libs.plugins.spring.boot)
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

group = "sm.selflearn"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Import BOMs to manage dependency versions
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}"))

    implementation(libs.spring.web)
    implementation(libs.spring.data.jpa)
    implementation(libs.spring.security.oauth2)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql.jdbc)
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(project(":shared:common-dtos"))


    // Keycloak Admin Client
    implementation(libs.keycloak.admin.client)

    // MinIO Client
    implementation(libs.minio)

    // TOML configuration support
    implementation(libs.jackson.dataformat.toml)

    // Micrometer Tracing
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.micrometer.registry.prometheus)

    // Logstash Encoder for JSON logging
    implementation(libs.logstash.logback.encoder)

    // Spring Boot Starter Mail for EmailService
    implementation(libs.spring.boot.starter.mail)

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
    testImplementation(libs.spring.security.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("sm.selflearn.samskrtam.user.Application")
}
