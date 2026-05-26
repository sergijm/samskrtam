
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
    implementation(libs.spring.web)
    implementation(libs.spring.data.jpa)
    implementation(libs.spring.security.oauth2)
    implementation(libs.postgresql.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.spring.kafka)
    implementation(libs.spring.redis)
    implementation(project(":shared:common-dto"))
    implementation(project(":shared:kafka-events"))

    testImplementation(libs.spring.test)
}

springBoot {
    mainClass = "sm.selflearn.samskrtam.quiz.vocabulary.Application"
}
