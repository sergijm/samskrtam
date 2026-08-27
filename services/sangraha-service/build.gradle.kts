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

    implementation(libs.spring.web)
    implementation(libs.spring.data.jpa)

    // Kafka удалён — sangraha ↔ content-service теперь через RestClient
    // implementation(libs.spring.kafka)

    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql.jdbc)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    implementation(libs.springdoc.openapi.webmvc.ui)

    implementation(libs.logstash.logback.encoder)
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.dataformat.yaml)

    // OpenAI SDK — замена самописного HTTP-клиента к LLM
    implementation("com.openai:openai-java:4.41.0")

    // NetworkNT JSON Schema validator
    implementation("com.networknt:json-schema-validator:1.5.0")

    // Транслитерация санскрита (TransliterationService) — в shared:samskrtam-commons
    implementation(project(":shared:samskrtam-commons"))

    implementation(project(":shared:samskrtam-dtos"))
    implementation(project(":shared:common-dtos"))

    testImplementation(libs.spring.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
