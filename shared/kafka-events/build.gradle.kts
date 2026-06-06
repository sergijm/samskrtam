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
    // Подключаем Spring Boot BOM для управления версиями
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

    // Зависимости для событий Kafka
    implementation(project(":shared:quiz-content-dtos"))

    // Зависимости для Jackson и Java 8 Date/Time (версии будут из BOM)
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Зависимости для Spring Kafka (используем псевдоним из libs.versions.toml)
    api(libs.spring.kafka)
    // kafka-clients будет транзитивно подключен через spring-kafka

    // Lombok (используем псевдоним из libs.versions.toml)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Логирование (версия будет из BOM)
    api("org.slf4j:slf4j-api")
}
