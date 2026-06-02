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
    implementation(libs.spring.r2dbc)
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactor)
    implementation(libs.postgresql.r2dbc)
    implementation(libs.flyway.core)
    implementation(libs.postgresql.jdbc)
    implementation("org.jsoup:jsoup:1.17.2")

    implementation(project(":shared:common-dtos")) // Обновлено на common-dtos
    implementation(project(":shared:dictionary-dtos"))

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.spring)
}
