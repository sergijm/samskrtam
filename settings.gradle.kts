rootProject.name = "samskrtam"

include(
    ":shared:user-dtos",
    ":shared:feature-flag-dtos",
    ":shared:samskrtam-dtos",
    ":shared:common-dtos",
    ":infrastructure:api-gateway",
    ":services:user-service",
    ":services:quiz-service",
    ":services:statistics-service",
    ":services:dictionary-service",
    ":services:sangraha-service",
    ":services:curriculum-service"
)

