rootProject.name = "samskrtam"

include(
    ":infrastructure:api-gateway",
    ":services:user-service",
    ":services:content-service",
    ":services:quiz-service",
    ":services:dictionary-service",
    ":services:statistics-service",
    ":shared:kafka-events",
    // ":shared:common-dto", // Удален старый модуль
    ":shared:user-dtos",
    ":shared:quiz-content-dtos",
    ":shared:dictionary-dtos",
    ":shared:statistics-dtos",
    ":shared:feature-flag-dtos",
    ":shared:quiz-dtos",
    ":shared:common-dtos" // Добавлен новый модуль
)
