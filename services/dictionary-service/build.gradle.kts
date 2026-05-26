plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.spring.webflux)
    implementation(libs.spring.r2dbc)
    implementation(libs.spring.security.oauth2)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactor)
    implementation(libs.postgresql.r2dbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(project(":shared:common-dto"))

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.spring)
}

springBoot {
    mainClass = "sm.selflearn.samskrtam.dictionary.ApplicationKt"
}
