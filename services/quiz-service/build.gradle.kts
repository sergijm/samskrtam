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
    
    implementation(project(":shared:common-dto"))
    implementation(project(":shared:kafka-events"))
    
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
    testImplementation(libs.spring.test)
}
