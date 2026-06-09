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

    // External Dependencies
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310") // For Java 8 Date/Time support
    api(libs.spring.kafka) // Spring Kafka
    api("org.slf4j:slf4j-api") // Logging

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
}

group = "sm.selflearn.samskrtam.quiz"
version = "0.0.1-SNAPSHOT"
