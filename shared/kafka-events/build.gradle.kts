plugins {
    id("java-library")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Зависимости для событий Kafka, если они понадобятся
    implementation(project(":shared:quiz-content-dtos"))
}
