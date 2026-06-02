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
    // Instant и UUID являются частью стандартной Java, spring-boot-starter не нужен.
    // Если потребуются другие общие зависимости, их можно добавить здесь.
}

group = "sm.selflearn.samskrtam.common"
version = "0.0.1-SNAPSHOT"
