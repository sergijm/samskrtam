
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
    implementation(platform("org.springframework.cloud:spring-cloud-dependencies:${libs.versions.spring.cloud.get()}"))
    implementation(libs.spring.webflux)
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.security.oauth2)
    implementation(libs.spring.redis.reactive)

}

springBoot {
    mainClass = "sm.selflearn.samskrtam.gateway.Application"
}
