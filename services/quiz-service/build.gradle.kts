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
    implementation(libs.postgresql.r2dbc)
    implementation(libs.postgresql.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.spring.kafka)
    implementation(libs.reactor.kafka)
    implementation(libs.spring.redis.reactive)

    implementation(libs.springdoc.openapi.webflux.ui)

    implementation(project(":shared:common-dtos"))
    implementation(project(":shared:quiz-dtos"))

        // MapStruct
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
    testImplementation(libs.reactor.test)
}
