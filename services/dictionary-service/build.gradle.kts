plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

    implementation(libs.spring.webflux)
    implementation(libs.kotlin.reflect) // Changed to libs alias
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactor)
    implementation(libs.flyway.core)
    implementation(libs.postgresql.jdbc)
    implementation(libs.jsoup) // Changed to libs alias

    implementation(project(":shared:common-dtos")) // Обновлено на common-dtos
    implementation(project(":shared:dictionary-dtos"))

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.spring)
}
