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
    implementation(libs.spring.r2dbc)
    implementation(libs.spring.kafka)
    implementation(libs.spring.redis.reactive)

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

    implementation(project(":shared:common-dtos")) // Обновлено на common-dtos
    implementation(project(":shared:kafka-events"))
    implementation(project(":shared:quiz-content-dtos"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
}
