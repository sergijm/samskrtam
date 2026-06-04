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
    implementation(libs.postgresql.r2dbc) // Added postgresql-r2dbc dependency
    implementation(libs.postgresql.jdbc) // Added postgresql-jdbc dependency for Flyway
    implementation(libs.flyway.core) // Added Flyway core dependency
    implementation(libs.flyway.postgresql) // Added Flyway PostgreSQL dependency
    implementation(libs.spring.kafka)
    implementation(libs.reactor.kafka) // Added reactor-kafka dependency
    implementation(libs.spring.redis.reactive) // Added spring-redis-reactive dependency

    implementation(libs.springdoc.openapi.webflux.ui) // Changed to libs alias

    implementation(project(":shared:common-dtos")) // Обновлено на common-dtos
    implementation(project(":shared:kafka-events"))
    implementation(project(":shared:quiz-content-dtos"))

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
}
