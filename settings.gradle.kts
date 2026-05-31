rootProject.name = "samskrtam"

include(
    ":infrastructure:api-gateway",
    ":services:user-service",
    ":services:content-service",
    ":services:quiz-service",
    ":services:dictionary-service",
    ":services:statistics-service",
    ":shared:kafka-events",
    ":shared:common-dto"
)
