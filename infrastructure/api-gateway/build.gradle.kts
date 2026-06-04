plugins {
    alias(libs.plugins.spring.boot)
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}"))

    implementation(libs.spring.cloud.gateway)

    implementation(libs.spring.security.oauth2)
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    implementation(libs.spring.redis.reactive)

    // Observability
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.micrometer.registry.prometheus)
    runtimeOnly(libs.logstash.logback.encoder)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(project(":shared:user-dtos"))

    testImplementation(libs.spring.test)
}
