# ADR-003 — Kotlin + Coroutines для dictionary-service

> Status: **Accepted** · Date: 2025

## Контекст
Проект на Java 21, но есть учебная цель освоить Kotlin в рамках проекта.

## Решение
dictionary-service реализован на Kotlin + Coroutines + R2DBC.

## Обоснование
dictionary-service — идеальный кандидат для Kotlin:
- Изолированный сервис без Kafka
- Cache-aside паттерн выразительно пишется на suspend fun:

```kotlin
suspend fun lookup(word: String): DictionaryEntry =
    repository.findByWord(word)
        ?: fetchFromExternal(word).also { repository.save(it) }
```

- WebClient для внешнего API естественно использовать с корутинами
- Минимальная зона риска — не затрагивает другие сервисы

## Последствия
**Плюсы:** практика Kotlin в реальном проекте, выразительный async код.
**Минусы:** два языка в монорепо, R2DBC вместо JPA.
