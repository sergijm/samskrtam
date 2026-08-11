# quiz-service: Архитектура, стек и ключевые решения

> Часть спецификации quiz-service. Основной файл: `docs/services/quiz-service.md`

---

## 1. Описание и назначение

Единый сервис для прохождения квизов всех типов (склонения, спряжения, лексика). Обрабатывает жизненный цикл сессии: старт, ответы, завершение. После завершения публикует события в Kafka через Outbox Pattern.

Разделение ответственности:
- curriculum-service — что есть в квизах (данные, настройки)
- quiz-service — как пользователь их проходит (сессии, ответы, события)

## 2. Стек

WebFlux выбран осознанно из-за интенсивного I/O: одновременные обращения к curriculum-service, запись в Postgres, публикация в Kafka. Реактивный pipeline позволяет держать всё в одном неблокирующем потоке.

Следствие: весь стек реактивный:
- База данных: R2DBC (не JPA/Hibernate)
- Kafka: ReactiveKafkaProducerTemplate
- HTTP-клиент: WebClient
- Flyway: добавляется JDBC datasource только для миграций (стандартная практика для R2DBC)

## 3. Зависимости

build.gradle.kts:
- spring-boot-webflux
- spring-boot-r2dbc
- r2dbc-postgresql
- spring-kafka
- flyway-core
- postgresql (для Flyway)
- samskrtam-dtos (shared)

## 4. Интеграция с curriculum-service (ContentClient)

WebClient для взаимодействия с curriculum-service:
- generateQuizData(UUID lessonId, String userLocale): Monо<GeneratedQuizData> — POST generate-quiz-data, вызывается только на start
- getDeclensionForms(UUID declensionStemId): Mono<List<DeclensionFormDto>> — для генерации дистракторов (вызывается на каждом рендере вопроса)
- getDeclensionStemsForLesson(String slug)
- getCaseEndingsByVowelType(String vowelType)
- getVocabularyWordsForLesson(UUID lessonId, int limit)
- getVocabularyWordById(UUID wordId)
- getLessonItem(UUID lessonId)
- getLessonItemBySlug(String slug)
- getQuizzesByCategory(String category)

Методы getGeneratedQuizData/getGeneratedQuestion удалены — resume/answer/complete читают вопросы из своей БД.

## 5. Депрекация correctAnswerRu/correctAnswerEn в пользу caseEnding

Поле caseEnding — единственный источник эталонного окончания для грамматических вопросов. correctAnswerRu/correctAnswerEn помечены @Deprecated, больше не заполняются, оставлены для обратной совместимости.

Frontend: должен использовать caseEnding для отображения правильного окончания.

## 6. Контракт вопроса для фронтенда — деванагари и перевод основы

Путь данных:
content.declension_stems.stem_devanagari/translation_ru/en -> DeclensionQuizGeneratorService копирует в QuestionResponse -> quiz-service сохраняет в session_questions -> оттуда отдаётся на StartSessionResponse/ResumeSessionResponse/AnswerResponse во фронтенд.

Поля (только для DECLENSIONS/CONJUGATIONS, для VOCABULARY пустые):
- stemDevanagari: string
- stemTranslationRu: string
- stemTranslationEn: string

Затронутые компоненты:
- curriculum-service: DeclensionStem, QuestionResponse, GeneratedQuizQuestionDto
- quiz-service: SessionQuestion, SessionQuestionMapper, миграция session_questions
- frontend: SessionQuestion в types/quiz.ts, рендер в QuizQuestionPanel.tsx

AnswerResponse без изменений — перевод основы для варианта ответа не требуется.