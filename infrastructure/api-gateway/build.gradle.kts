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

    // The gateway acts as both a Resource Server (validates tokens) and a Client (initiates login)
    implementation(libs.spring.security.oauth2) // spring-boot-starter-oauth2-resource-server
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client") // The correct starter for the login flow

    // Redis for OAuth2 state management
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // Observability
    implementation("org.springframework.boot:spring-boot-starter-actuator") // Added for ObservationRegistry auto-configuration
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("net.logstash.logback:logstash-logback-encoder:7.4")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
}
