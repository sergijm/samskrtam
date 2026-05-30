rootProject.name = "samskrtam"

include(
    ":infrastructure:api-gateway",
    ":services:auth-service", // Исправленный путь
    ":services:content-service",
    ":services:quiz-service",
    ":services:dictionary-service",
    ":services:statistics-service",
    ":shared:kafka-events",
    ":shared:common-dto"
)
