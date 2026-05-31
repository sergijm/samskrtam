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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql.jdbc)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")
    implementation(project(":shared:common-dto"))

    // Keycloak Admin Client
    implementation("org.keycloak:keycloak-admin-client:24.0.4")

    // Micrometer Tracing (versions managed by Spring Boot BOM)
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Logstash Encoder for JSON logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("sm.selflearn.samskrtam.user.Application")
}
