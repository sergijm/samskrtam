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
    
    implementation(libs.spring.webflux) // Changed from libs.spring.web
    implementation(libs.spring.data.jpa) // Keep JPA for now, but note that R2DBC is preferred for reactive
    // Removed implementation("org.springframework.boot:spring-boot-starter-security-reactive") - this line is already commented out
    
    // Springdoc OpenAPI for WebFlux
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql.jdbc)
    
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    testImplementation(libs.spring.test)
}
