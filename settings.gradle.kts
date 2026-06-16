rootProject.name = "samskrtam"

include(
    ":infrastructure:api-gateway",
    ":services:user-service",
    ":services:content-service",
    ":services:quiz-service",
    ":services:statistics-service",
    ":services:dictionary-service", // Added dictionary-service
    ":shared:user-dtos",
    ":shared:feature-flag-dtos",
    ":shared:quiz-dtos",
    ":shared:common-dtos"
)
