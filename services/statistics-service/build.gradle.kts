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
    implementation(libs.postgresql.jdbc)
    implementation(libs.flyway.core)

    implementation(project(":shared:common-dtos")) // Обновлено на common-dtos
    implementation(project(":shared:kafka-events"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
}
