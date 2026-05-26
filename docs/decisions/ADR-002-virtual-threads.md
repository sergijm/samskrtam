# ADR-002 — Java 21 Virtual Threads для большинства сервисов

> Status: **Accepted** · Date: 2025

## Контекст
Выбор async модели для Java сервисов. Альтернативы: WebFlux/Reactor, Virtual Threads.

## Решение
Java 21 Virtual Threads (Project Loom) для всех Java сервисов кроме API Gateway.

## Обоснование
Virtual Threads позволяют писать обычный блокирующий код — JVM автоматически делает его неблокирующим:
- Обычный Spring MVC вместо WebFlux
- Обычный JDBC/JPA вместо R2DBC
- Читаемый синхронный код без flatMap/Mono
- Gemini лучше генерирует Java MVC чем Java WebFlux

```yaml
# Включается одной строкой:
spring:
  threads:
    virtual:
      enabled: true
```

## Исключения
- **API Gateway** — WebFlux обязателен (Spring Cloud Gateway построен на Reactor)
- **dictionary-service** — Kotlin + Coroutines (учебная цель)

## Последствия
**Плюсы:** простой код, лучшая генерация моделью, обычный JPA/JDBC.
**Минусы:** нельзя использовать synchronized блоки с VT (pinning проблема).
