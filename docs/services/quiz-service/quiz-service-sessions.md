# quiz-service: Механика сессий

> Часть спецификации quiz-service. Основной файл: `docs/services/quiz-service.md`

---

## 1. Поддерживаемые типы квизов

Сервис поддерживает все типы квизов платформы: склонения (DECLENSIONS), спряжения (CONJUGATIONS), лексика (VOCABULARY). Тип квиза определяется itemType quest-единиц топика.

## 2. Жизненный цикл сессии

Сессия последовательно проходит состояния: IN_PROGRESS → COMPLETED.

### Старт сессии (compose)

Клиент отправляет POST `/api/v2/quiz/compose` с `QuestComposeRequest`. Сервис:
- Получает лёгкий пул топика у curriculum-service (id, itemType, progressTag)
- Выполняет прогресс-отбор через `QuizGenerator` (due/new/reserve)
- Отправляет отобранные itemIds в curriculum-service на композицию (материализация prompt + correctAnswer + distractors + payload)
- Сохраняет сессию в `quiz_session` со статусом IN_PROGRESS
- Сохраняет вопросы в `session_questions` (опции фиксируются при старте)
- Публикует `QuizSessionStatusChangedEvent` (status=IN_PROGRESS) через Outbox
- Возвращает `ComposeQuizResponse` с первым вопросом и вариантами

### Возобновление сессии (resume)

Клиент отправляет GET с идентификатором сессии. Сервис:
- Восстанавливает `QuizSession` по ID из БД
- Загружает вопросы из `session_questions` (curriculum-service для этого не вызывается)
- Регидратирует опции через `ComposedQuestionMapper` (детерминированные id)
- Возвращает `StartOrResumeResponse` с текущим вопросом

### Ответ на вопрос (answer)

Клиент отправляет POST с идентификатором сессии и `AnswerRequest`. Сервис:
- Проверяет существование сессии и принадлежность пользователю
- Проверяет что вопрос ещё не был отвечен
- Загружает детали вопроса из `session_questions`
- Сравнивает выбранную опцию с `correctAnswer` (или проверяет match-сабмишены)
- Сохраняет ответ в `quiz_answers` и обновляет сессию (answered_questions, score)
- Публикует `QuizAnsweredEvent` через Outbox
- Возвращает `AnswerResponse`

### Завершение сессии (complete)

Клиент отправляет POST с идентификатором сессии. Сервис:
- Проверяет существование и принадлежность
- Загружает все ответы и вопросы
- Обновляет статус на COMPLETED
- Публикует `QuizSessionStatusChangedEvent` через Outbox
- Возвращает `CompleteSessionResponse`

## 3. Реактивный pipeline (QuizSessionService)

Все методы — реактивные цепочки Reactor (Mono/Flux) с R2DBC, WebClient и ReactiveKafkaProducerTemplate. Ключевой принцип — атомарность: запись в БД и публикация Outbox в одной транзакционной цепочке.

Методы:
- `startOrResumeSessionByTopic`: compose в curriculum-service, сохранение сессии и вопросов, Outbox
- `resumeSession`: восстановление из БД, регидратация опций
- `submitAnswer`: проверка, сохранение, Outbox
- `completeSession`: завершение, Outbox
- `retakeSession`: сброс ответов, новый показ вопросов