plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Управление версиями Spring Boot через BOM
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

    // Spring Context для @Configuration / @Bean
    api("org.springframework:spring-context")

    // Sanscript.java для транслитерации санскрита (IAST ↔ SLP1 ↔ Devanagari)
    api("com.github.sanskrit:sanscript.java:0.1")
}

group = "sm.selflearn.samskrtam.common"
version = "0.0.1-SNAPSHOT"
