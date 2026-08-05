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

    implementation(libs.spring.web)
    implementation(libs.spring.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.springdoc.openapi.webmvc.ui)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql.jdbc)
    implementation("org.springframework.boot:spring-boot-starter-validation")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    annotationProcessor(libs.lombok.mapstruct.binding)

    testImplementation(libs.spring.test)
}

springBoot {
    mainClass.set("sm.selflearn.samskrtam.curriculum.CurriculumServiceApplication")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
