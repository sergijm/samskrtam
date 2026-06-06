plugins {
    id("java-library")
    id("org.springframework.boot") apply false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral() // Добавлен репозиторий
}

dependencies {
    // Управление версиями Spring Boot через BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    implementation(project(":shared:common-dtos"))
}

group = "sm.selflearn.samskrtam.user"
version = "0.0.1-SNAPSHOT"
