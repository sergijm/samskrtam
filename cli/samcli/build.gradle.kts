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

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-jdbc")
    implementation(libs.postgresql.jdbc)
    implementation(libs.picocli)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.databind)
    implementation(project(":shared:samskrtam-dtos"))

    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Bundle the root samcli.yml as a classpath resource so the tool always has a
// default config even when launched from a different working directory.
tasks.processResources {
    from(rootProject.projectDir) {
        include("samcli.yml")
    }
}
