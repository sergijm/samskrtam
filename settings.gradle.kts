rootProject.name = "samskrtam"

include(
    ":shared:user-dtos",
    ":shared:samskrtam-dtos",
    ":shared:common-dtos",
    ":shared:samskrtam-commons",
    ":infrastructure:api-gateway",
    ":services:user-service",
    ":services:quiz-service",
    ":services:dictionary-service",
    ":services:sangraha-service",
    ":services:curriculum-service",
    ":cli:samcli"
)

