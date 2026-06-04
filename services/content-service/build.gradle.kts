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

    implementation(libs.spring.webflux)
    implementation(libs.spring.data.jpa)

    implementation(libs.springdoc.openapi.webflux.ui) // Changed to libs alias

    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql.jdbc)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(project(":shared:quiz-content-dtos"))
    implementation(project(":shared:common-dtos")) // Обновлено на common-dtos

    testImplementation(libs.spring.test)
}
