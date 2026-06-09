plugins {
    id("java-library")
    id("org.springframework.boot") apply false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Управление версиями Spring Boot через BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

    // Project Dependencies
    implementation(project(":shared:common-dtos"))

    // External Dependencies (DTOs, Validation, Kafka Events)
    api("jakarta.validation:jakarta.validation-api:3.0.2") // Used in DTOs
    api("com.fasterxml.jackson.core:jackson-annotations") // Managed by BOM
    api("com.fasterxml.jackson.core:jackson-databind")    // Managed by BOM
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310") // For Java 8 Date/Time support
    api(libs.spring.kafka) // Spring Kafka for event classes
    api("org.slf4j:slf4j-api") // Logging for event classes

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

group = "sm.selflearn.samskrtam.quiz"
version = "0.0.1-SNAPSHOT"
