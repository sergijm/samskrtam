plugins {
    id("java-library") // Используем java-library для транзитивных зависимостей
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Пример: api("jakarta.validation:jakarta.validation-api:3.0.2")
    // Используем 'api' вместо 'implementation', чтобы проекты,
    // которые зависят от common-dto, тоже видели эти зависимости.
    
    // Пока зависимостей нет
}
