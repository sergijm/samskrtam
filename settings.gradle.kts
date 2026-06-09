rootProject.name = "samskrtam"

include(
    ":infrastructure:api-gateway",
    ":services:user-service",
    ":services:content-service",
    ":services:quiz-service",
    ":services:dictionary-service",
    ":services:statistics-service",
    ":shared:user-dtos",
    ":shared:dictionary-dtos",
    ":shared:feature-flag-dtos",
    ":shared:quiz-dtos", // Объединенный модуль
    ":shared:common-dtos"
)
