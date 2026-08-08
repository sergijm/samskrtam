# quiz-service: Механика сессий

> Часть спецификации quiz-service. Основной файл: `docs/services/quiz-service.md`

---

## 1. Поддерживаемые типы квизов

Сервис поддерживает все типы квизов платформы: склонения (DECLENSIONS), спряжения (CONJUGATIONS), лексика (VOCABULARY). Тип квиза указывается в пути эндпоинта и определяет логику генерации вопросов и проверки ответов.

## 2. Жизненный цикл сессии

Сессия последовательно проходит три состояния: IN_PROGRESS, COMPLETED (возможно FAILED в будущем).

### Старт сессии

Клиент отправляет GET-запрос с типом квиза и опциональным quizId. Сервис:
- Вызывает curriculum-service (POST generate-quiz-data) для генерации набора вопросов.
- Сохраняет сессию в таблицу quiz_session со статусом IN_PROGRESS.
- Сохраняет полный список вопросов в таблицу session_questions — это единственное персистентное хранилище вопросов сессии.
- Для DECLENSIONS/CONJUGATIONS генерирует варианты ответа (дистракторы) на лету с помощью DeclensionOptionGeneratorService / LexicalOptionGeneratorService (см. описание дистракторов в файле quiz-service-repositories.md). Дистракторы не сохраняются.
- Публикует событие QuizSessionStatusChangedEvent (status=IN_PROGRESS) через Outbox Pattern.
- Возвращает StartSessionResponse, содержащий первый вопрос с вариантами.

### Возобновление сессии

Клиент отправляет GET-запрос с идентификатором сессии. Сервис:
- Восстанавливает QuizSession по ID из БД.
- Загружает вопросы сессии из session_questions (curriculum-service для этого не вызывается).
- Для текущего вопроса генерирует дистракторы заново — здесь вызывается curriculum-service (getDeclensionForms) для получения форм для дистракторов.
- Возвращает ResumeSessionResponse с текущим вопросом и вариантами.

### Ответ на вопрос

Клиент отправляет POST-запрос с идентификатором сессии и телом AnswerRequest. Сервис:
- Проверяет существование сессии и принадлежность пользователю.
- Проверяет, что вопрос ещё не был отвечен.
- Загружает детали вопроса из session_questions (не из curriculum-service).
- Проверяет правильность ответа по строковому сравнению (selected_form_iast / correct_form_iast). Для VOCABULARY учитывается targetLanguage.
- Сохраняет ответ в quiz_answers и обновляет сессию (answered_questions, score).
- Публикует QuizAnsweredEvent через Outbox Pattern.
- Возвращает AnswerResponse с результатом и, если квиз завершён, следующим вопросом.

### Завершение сессии

Клиент отправляет POST-запрос с идентификатором сессии. Сервис:
- Проверяет существование сессии и принадлежность пользователю.
- Загружает все ответы и вопросы сессии.
- Обновляет статус сессии на COMPLETED.
- Публикует QuizSessionStatusChangedEvent (status=COMPLETED) через Outbox Pattern.
- Возвращает CompleteSessionResponse с итоговой статистикой.

### Проверка прогресса

Клиент отправляет GET-запрос с userId и quizId. Сервис находит последнюю незавершённую сессию для этой пары и возвращает QuizProgressDto.

## 3. Реактивный pipeline (QuizSessionService)

Все методы сервиса строятся как реактивные цепочки Reactor (Mono/Flux) с использованием R2DBC, WebClient и ReactiveKafkaProducerTemplate. Ключевой принцип — атомарность: запись в БД и публикация события Outbox выполняются в рамках одной транзакционной цепочки.

Методы:
- startSession: получает вопросы от curriculum-service, сохраняет сессию и вопросы, публикует Outbox-событие.
- resumeSession: восстанавливает сессию и вопросы, генерирует дистракторы с вызовом curriculum-service.
- submitAnswer: проверяет ответ, сохраняет, публикует Outbox-событие.
- completeSession: завершает сессию, публикует Outbox-событие.