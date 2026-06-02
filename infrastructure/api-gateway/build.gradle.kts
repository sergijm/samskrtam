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

    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(project(":shared:user-dtos"))

    testImplementation(libs.spring.test)
}
