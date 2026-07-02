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
    implementation(libs.spring.kafka)
    implementation(libs.spring.kafka.streams) // Use libs alias for Kafka Streams dependency
    implementation(libs.postgresql.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)

    // Добавляем зависимость для Springdoc OpenAPI
    implementation(libs.springdoc.openapi.webmvc.ui)

    // Добавляем зависимость для поддержки Java 8 Time API в Jackson
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation(project(":shared:common-dtos"))
    implementation(project(":shared:samskrtam-dtos"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
}
