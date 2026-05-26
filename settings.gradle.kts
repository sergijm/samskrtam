
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "samskrtam-app"

include(
    ":infrastructure:api-gateway",
    ":services:content-service",
    ":services:quiz-declensions-service",
    ":services:quiz-conjugations-service",
    ":services:quiz-vocabulary-service",
    ":services:dictionary-service",
    ":services:statistics-service",
    ":shared:kafka-events",
    ":shared:common-dto"
)
