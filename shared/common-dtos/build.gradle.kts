plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Управление версиями Spring Boot через BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

    // Зависимости для Jackson
    api(libs.jackson.datatype.jsr310)
    api(libs.jackson.databind)
    api(libs.jackson.annotations)
    api(libs.jackson.core)

    // Зависимость для Spring Context, чтобы использовать @Configuration и @Bean
    api("org.springframework:spring-context")

    // Jakarta Validation API (аннотации @NotBlank, @Size и т.д.)
    api("jakarta.validation:jakarta.validation-api")

    // Lombok
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}

group = "sm.selflearn.samskrtam.common"
version = "0.0.1-SNAPSHOT"
